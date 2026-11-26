package albaguard.storage;

import albaguard.domain.BusinessSize;
import albaguard.domain.Contract;
import albaguard.domain.Deduction;
import albaguard.domain.EarlyLeaveReason;
import albaguard.domain.PayItem;
import albaguard.domain.ReceivedPay;
import albaguard.domain.WorkRecord;
import albaguard.service.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRecordServiceTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);

    @TempDir
    Path dir;

    private Path file;
    private JsonRecordService service;

    @BeforeEach
    void setUp() {
        file = dir.resolve("data").resolve("albaguard.json");
        service = new JsonRecordService(file);
    }

    private static Contract contract() {
        return new Contract("편의점", 10_320, LocalDate.of(2026, 9, 1), null,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                LocalTime.of(18, 0), LocalTime.of(23, 0), 30, false, null,
                BusinessSize.FIVE_OR_MORE, true);
    }

    private static WorkRecord record(String id, LocalDate date, String in, String out) {
        return new WorkRecord(id, date, LocalTime.parse(in), LocalTime.parse(out), 30, false,
                EarlyLeaveReason.NONE, "메모");
    }

    @Test
    void 근로_조건을_저장하고_새_객체로_열어도_같은_값() {
        service.saveContract(contract());

        JsonRecordService reopened = new JsonRecordService(file);
        assertEquals(contract(), reopened.getContract().orElseThrow());
    }

    @Test
    void 저장_전에는_근로_조건이_없다() {
        assertTrue(service.getContract().isEmpty());
    }

    @Test
    void 근무_기록은_해당_월에만_나오고_날짜순이다() {
        service.saveContract(contract());
        service.addRecord(record("b", LocalDate.of(2026, 11, 4), "18:00", "23:00"));
        service.addRecord(record("a", LocalDate.of(2026, 11, 2), "18:00", "23:00"));
        service.addRecord(record("c", LocalDate.of(2026, 12, 1), "18:00", "23:00"));

        assertEquals(List.of("a", "b"), service.getRecords(NOV).stream().map(WorkRecord::id).toList());
        assertEquals(List.of("c"), service.getRecords(DEC).stream().map(WorkRecord::id).toList());
        assertTrue(service.getRecords(YearMonth.of(2027, 1)).isEmpty());
    }

    @Test
    void 근무_기록은_파일에서_다시_읽어도_같다() {
        service.saveContract(contract());
        WorkRecord r = record("a", LocalDate.of(2026, 11, 2), "22:00", "02:00");
        service.addRecord(r);

        assertEquals(List.of(r), new JsonRecordService(file).getRecords(NOV));
    }

    @Test
    void 근로_조건이_없으면_근무_기록_추가_불가() {
        assertThrows(ValidationException.class,
                () -> service.addRecord(record("a", LocalDate.of(2026, 11, 2), "18:00", "23:00")));
    }

    @Test
    void 없는_id로_수정하면_예외() {
        service.saveContract(contract());
        assertThrows(ValidationException.class,
                () -> service.updateRecord(record("none", LocalDate.of(2026, 11, 2), "18:00", "23:00")));
    }

    @Test
    void 수정하면_같은_id의_기록이_교체된다() {
        service.saveContract(contract());
        service.addRecord(record("a", LocalDate.of(2026, 11, 2), "18:00", "23:00"));
        WorkRecord changed = record("a", LocalDate.of(2026, 11, 2), "18:00", "23:30");
        service.updateRecord(changed);

        assertEquals(List.of(changed), service.getRecords(NOV));
    }

    @Test
    void 삭제하면_사라지고_없는_id_삭제는_무시() {
        service.saveContract(contract());
        service.addRecord(record("a", LocalDate.of(2026, 11, 2), "18:00", "23:00"));
        service.deleteRecord("none");
        assertEquals(1, service.getRecords(NOV).size());

        service.deleteRecord("a");
        assertTrue(service.getRecords(NOV).isEmpty());
        assertTrue(new JsonRecordService(file).getRecords(NOV).isEmpty());
    }

    @Test
    void 같은_날_여러_건과_중복_id() {
        service.saveContract(contract());
        service.addRecord(record("a", LocalDate.of(2026, 11, 2), "09:00", "12:00"));
        service.addRecord(record("b", LocalDate.of(2026, 11, 2), "18:00", "23:00"));
        assertEquals(2, service.getRecords(NOV).size());

        assertThrows(ValidationException.class,
                () -> service.addRecord(record("a", LocalDate.of(2026, 11, 3), "18:00", "23:00")));
    }

    @Test
    void 잘못된_근무_기록은_예외() {
        service.saveContract(contract());
        LocalDate d = LocalDate.of(2026, 11, 2);
        ValidationException same = assertThrows(ValidationException.class,
                () -> service.addRecord(record("a", d, "18:00", "18:00")));
        assertEquals("퇴근 시각이 출근 시각과 같습니다.", same.getMessage());

        assertThrows(ValidationException.class, () -> service.addRecord(new WorkRecord(
                "b", d, LocalTime.of(18, 0), LocalTime.of(23, 0), -1, false, EarlyLeaveReason.NONE, "")));
        assertThrows(ValidationException.class, () -> service.addRecord(new WorkRecord(
                "c", d, LocalTime.of(18, 0), LocalTime.of(19, 0), 60, false, EarlyLeaveReason.NONE, "")));
    }

    @Test
    void 잘못된_근로_조건은_예외() {
        Contract c = contract();
        assertThrows(ValidationException.class, () -> service.saveContract(new Contract(
                c.workplaceName(), 0, c.startDate(), null, c.workDays(), c.scheduledStart(),
                c.scheduledEnd(), 30, false, null, c.businessSize(), true)));
        assertThrows(ValidationException.class, () -> service.saveContract(new Contract(
                c.workplaceName(), 10_320, c.startDate(), null, Set.of(), c.scheduledStart(),
                c.scheduledEnd(), 30, false, null, c.businessSize(), true)));
        assertTrue(service.getContract().isEmpty());
    }

    @Test
    void 공제_금액이_0_이하면_예외_정상이면_월별_조회() {
        Deduction bad = new Deduction("d0", LocalDate.of(2026, 11, 5), 0, "벌금");
        assertThrows(ValidationException.class, () -> service.addDeduction(bad));

        service.addDeduction(new Deduction("d1", LocalDate.of(2026, 11, 5), 10_000, "지각 벌금"));
        service.addDeduction(new Deduction("d2", LocalDate.of(2026, 12, 5), 5_000, "유니폼비"));
        assertEquals(1, service.getDeductions(NOV).size());
        assertEquals(1, new JsonRecordService(file).getDeductions(DEC).size());

        service.deleteDeduction("d1");
        assertTrue(service.getDeductions(NOV).isEmpty());
    }

    @Test
    void 받은_금액_미입력_월은_빈_Optional() {
        assertTrue(service.getReceivedPay(NOV).isEmpty());
    }

    @Test
    void 받은_금액은_같은_월이면_덮어쓰고_파일에서도_복원() {
        service.saveReceivedPay(new ReceivedPay(NOV, 800_000, 20_000, Map.of()));
        ReceivedPay second = new ReceivedPay(NOV, 838_590, 28_610,
                Map.of(PayItem.BASE, 789_480, PayItem.WEEKLY_HOLIDAY, 0));
        service.saveReceivedPay(second);

        assertEquals(second, new JsonRecordService(file).getReceivedPay(NOV).orElseThrow());
        assertTrue(service.getReceivedPay(DEC).isEmpty());
    }

    @Test
    void 음수_받은_금액은_예외() {
        assertThrows(ValidationException.class,
                () -> service.saveReceivedPay(new ReceivedPay(NOV, -1, 0, Map.of())));
        assertThrows(ValidationException.class,
                () -> service.saveReceivedPay(new ReceivedPay(NOV, 0, -1, Map.of())));
    }

    @Test
    void 저장_파일은_사람이_읽을_수_있는_JSON() throws IOException {
        service.saveContract(contract());
        service.addRecord(record("a", LocalDate.of(2026, 11, 2), "18:00", "23:00"));

        String json = Files.readString(file);
        assertTrue(json.contains("\n"), "줄바꿈 들여쓰기");
        assertTrue(json.contains("\"workplaceName\" : \"편의점\""));
        assertTrue(json.contains("\"date\" : \"2026-11-02\""));
        assertTrue(json.contains("\"clockIn\" : \"18:00:00\""));
        assertTrue(json.contains("\"businessSize\" : \"FIVE_OR_MORE\""));
    }

    @Test
    void 깨진_파일은_읽기_예외() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ 이건 JSON이 아님");
        assertThrows(UncheckedIOException.class, () -> new JsonRecordService(file));
    }
}
