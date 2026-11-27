package albaguard.storage;

import albaguard.domain.Contract;
import albaguard.domain.Deduction;
import albaguard.domain.ReceivedPay;
import albaguard.domain.WorkRecord;
import albaguard.engine.WorkTime;
import albaguard.service.RecordService;
import albaguard.service.ValidationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * RecordService의 JSON 파일 구현.
 * 시작할 때 파일을 읽어 메모리에 올리고, 저장·수정·삭제할 때마다 파일 전체를 다시 쓴다.
 * 파일 쓰기에 실패하면 예외를 던지고 메모리 상태도 바꾸지 않는다.
 */
public class JsonRecordService implements RecordService {

    /** 파일에 저장되는 전체 데이터. 불변이며 변경 시 새 객체로 교체한다. */
    record Store(
            Contract contract,
            List<WorkRecord> records,
            List<Deduction> deductions,
            List<ReceivedPay> receivedPays) {

        Store {
            records = records == null ? List.of() : List.copyOf(records);
            deductions = deductions == null ? List.of() : List.copyOf(deductions);
            receivedPays = receivedPays == null ? List.of() : List.copyOf(receivedPays);
        }

        static Store empty() {
            return new Store(null, null, null, null);
        }
    }

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private Store store;

    /**
     * @param file 저장 파일 경로. 없으면 빈 상태로 시작하고 첫 저장 때 만든다.
     * @throws UncheckedIOException 파일은 있는데 읽을 수 없거나 형식이 깨진 경우
     */
    public JsonRecordService(Path file) {
        this.file = file;
        this.store = load();
    }

    // ---- 근로 조건 ----

    @Override
    public synchronized Optional<Contract> getContract() {
        return Optional.ofNullable(store.contract());
    }

    @Override
    public synchronized void saveContract(Contract contract) {
        validateContract(contract);
        persist(new Store(contract, store.records(), store.deductions(), store.receivedPays()));
    }

    // ---- 근무 기록 ----

    @Override
    public synchronized List<WorkRecord> getRecords(YearMonth month) {
        return store.records().stream()
                .filter(r -> YearMonth.from(r.date()).equals(month))
                .sorted(Comparator.comparing(WorkRecord::date).thenComparing(WorkRecord::clockIn))
                .toList();
    }

    @Override
    public synchronized void addRecord(WorkRecord record) {
        if (store.contract() == null) {
            throw new ValidationException("근로 조건을 먼저 저장해 주세요.");
        }
        validateRecord(record);
        if (store.records().stream().anyMatch(r -> r.id().equals(record.id()))) {
            throw new ValidationException("이미 있는 근무 기록 id입니다.");
        }
        List<WorkRecord> records = new ArrayList<>(store.records());
        records.add(record);
        persist(new Store(store.contract(), records, store.deductions(), store.receivedPays()));
    }

    @Override
    public synchronized void updateRecord(WorkRecord record) {
        validateRecord(record);
        List<WorkRecord> records = new ArrayList<>(store.records());
        int index = indexOfRecord(records, record.id());
        if (index < 0) {
            throw new ValidationException("수정할 근무 기록을 찾을 수 없습니다.");
        }
        records.set(index, record);
        persist(new Store(store.contract(), records, store.deductions(), store.receivedPays()));
    }

    @Override
    public synchronized void deleteRecord(String recordId) {
        List<WorkRecord> records = new ArrayList<>(store.records());
        if (records.removeIf(r -> r.id().equals(recordId))) {
            persist(new Store(store.contract(), records, store.deductions(), store.receivedPays()));
        }
    }

    // ---- 공제 ----

    @Override
    public synchronized List<Deduction> getDeductions(YearMonth month) {
        return store.deductions().stream()
                .filter(d -> YearMonth.from(d.date()).equals(month))
                .sorted(Comparator.comparing(Deduction::date))
                .toList();
    }

    @Override
    public synchronized void addDeduction(Deduction deduction) {
        if (deduction == null) {
            throw new ValidationException("공제 기록이 없습니다.");
        }
        if (isBlank(deduction.id())) {
            throw new ValidationException("공제 기록 id가 없습니다.");
        }
        if (deduction.date() == null) {
            throw new ValidationException("공제 날짜를 입력해 주세요.");
        }
        if (deduction.amount() <= 0) {
            throw new ValidationException("공제 금액은 0원보다 커야 합니다.");
        }
        if (store.deductions().stream().anyMatch(d -> d.id().equals(deduction.id()))) {
            throw new ValidationException("이미 있는 공제 기록 id입니다.");
        }
        List<Deduction> deductions = new ArrayList<>(store.deductions());
        deductions.add(deduction);
        persist(new Store(store.contract(), store.records(), deductions, store.receivedPays()));
    }

    @Override
    public synchronized void deleteDeduction(String deductionId) {
        List<Deduction> deductions = new ArrayList<>(store.deductions());
        if (deductions.removeIf(d -> d.id().equals(deductionId))) {
            persist(new Store(store.contract(), store.records(), deductions, store.receivedPays()));
        }
    }

    // ---- 받은 금액 ----

    @Override
    public synchronized Optional<ReceivedPay> getReceivedPay(YearMonth month) {
        return store.receivedPays().stream().filter(p -> p.month().equals(month)).findFirst();
    }

    @Override
    public synchronized void saveReceivedPay(ReceivedPay pay) {
        if (pay == null || pay.month() == null) {
            throw new ValidationException("받은 금액의 대상 월을 입력해 주세요.");
        }
        if (pay.totalAmount() < 0) {
            throw new ValidationException("실제 입금액은 0원 이상이어야 합니다.");
        }
        if (pay.taxAndInsurance() < 0) {
            throw new ValidationException("세금·보험 공제액은 0원 이상이어야 합니다.");
        }
        if (pay.breakdown().values().stream().anyMatch(v -> v < 0)) {
            throw new ValidationException("명세서 항목 금액은 0원 이상이어야 합니다.");
        }
        List<ReceivedPay> pays = new ArrayList<>(store.receivedPays());
        pays.removeIf(p -> p.month().equals(pay.month()));
        pays.add(pay);
        persist(new Store(store.contract(), store.records(), store.deductions(), pays));
    }

    // ---- 검증 ----

    private static void validateContract(Contract c) {
        if (c == null) {
            throw new ValidationException("근로 조건이 없습니다.");
        }
        if (c.hourlyWage() <= 0) {
            throw new ValidationException("시급은 0원보다 커야 합니다.");
        }
        if (c.startDate() == null) {
            throw new ValidationException("계약 시작일을 입력해 주세요.");
        }
        if (c.endDate() != null && c.endDate().isBefore(c.startDate())) {
            throw new ValidationException("계약 종료일이 시작일보다 빠릅니다.");
        }
        if (c.workDays().isEmpty()) {
            throw new ValidationException("근무 요일을 하나 이상 선택해 주세요.");
        }
        if (c.scheduledStart() == null || c.scheduledEnd() == null) {
            throw new ValidationException("계약 출퇴근 시각을 입력해 주세요.");
        }
        if (c.scheduledStart().equals(c.scheduledEnd())) {
            throw new ValidationException("퇴근 시각이 출근 시각과 같습니다.");
        }
        if (c.scheduledBreakMinutes() < 0) {
            throw new ValidationException("휴게시간은 0분 이상이어야 합니다.");
        }
        if (WorkTime.workMinutes(c.scheduledStart(), c.scheduledEnd(), c.scheduledBreakMinutes()) <= 0) {
            throw new ValidationException("휴게시간이 근무시간보다 깁니다.");
        }
        if (c.businessSize() == null) {
            throw new ValidationException("사업장 규모를 선택해 주세요.");
        }
    }

    private static void validateRecord(WorkRecord r) {
        if (r == null) {
            throw new ValidationException("근무 기록이 없습니다.");
        }
        if (isBlank(r.id())) {
            throw new ValidationException("근무 기록 id가 없습니다.");
        }
        if (r.date() == null) {
            throw new ValidationException("근무 날짜를 입력해 주세요.");
        }
        if (r.clockIn() == null || r.clockOut() == null) {
            throw new ValidationException("출퇴근 시각을 입력해 주세요.");
        }
        if (r.clockIn().equals(r.clockOut())) {
            throw new ValidationException("퇴근 시각이 출근 시각과 같습니다.");
        }
        if (r.breakMinutes() < 0) {
            throw new ValidationException("휴게시간은 0분 이상이어야 합니다.");
        }
        if (WorkTime.workMinutes(r) <= 0) {
            throw new ValidationException("휴게시간이 근무시간보다 깁니다.");
        }
        if (r.earlyLeaveReason() == null) {
            throw new ValidationException("조기 퇴근 사유를 선택해 주세요.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static int indexOfRecord(List<WorkRecord> records, String id) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    // ---- 파일 입출력 ----

    private Store load() {
        if (!Files.exists(file)) {
            return Store.empty();
        }
        try {
            Store loaded = mapper.readValue(file.toFile(), Store.class);
            return loaded == null ? Store.empty() : loaded;
        } catch (IOException e) {
            throw new UncheckedIOException("저장 파일을 읽을 수 없습니다: " + file, e);
        }
    }

    /** 임시 파일에 먼저 쓴 뒤 옮겨서, 쓰다 실패해도 기존 파일이 깨지지 않게 한다. 성공해야 메모리 상태를 바꾼다. */
    private void persist(Store next) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writeValue(tmp.toFile(), next);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("저장 파일을 쓸 수 없습니다: " + file, e);
        }
        this.store = next;
    }
}
