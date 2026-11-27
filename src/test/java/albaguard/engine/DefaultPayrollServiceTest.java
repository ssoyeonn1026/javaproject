package albaguard.engine;

import albaguard.domain.BusinessSize;
import albaguard.domain.Contract;
import albaguard.domain.EarlyLeaveReason;
import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.domain.PayrollResult;
import albaguard.domain.WorkRecord;
import albaguard.service.ValidationException;
import albaguard.storage.JsonRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPayrollServiceTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);
    private static final Set<DayOfWeek> MON_WED_FRI_SAT =
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    @TempDir
    Path dir;

    private JsonRecordService records;
    private DefaultPayrollService service;

    @BeforeEach
    void setUp() {
        records = new JsonRecordService(dir.resolve("albaguard.json"));
        service = new DefaultPayrollService(records);
    }

    private static Contract contract(BusinessSize size) {
        // 11/2 시작: 11/1(일)로 끝나는 주(10/26~11/1)는 계약 전이라 주휴수당 대상이 아니다
        return new Contract("편의점", 10_320, LocalDate.of(2026, 11, 2), null, MON_WED_FRI_SAT,
                LocalTime.of(18, 0), LocalTime.of(23, 0), 30, false, null, size, true);
    }

    private void work(LocalDate date) {
        records.addRecord(new WorkRecord("r-" + date, date, LocalTime.of(18, 0), LocalTime.of(23, 0), 30,
                false, EarlyLeaveReason.NONE, ""));
    }

    /** 계약 요일(월·수·금·토)마다 출근한 기록을 from~to 사이에 모두 만든다. */
    private void workAllContractDays(LocalDate from, LocalDate to) {
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (MON_WED_FRI_SAT.contains(d.getDayOfWeek())) {
                work(d);
            }
        }
    }

    private static int amountOf(PayrollResult result, PayItem item) {
        return result.lines().stream().filter(l -> l.item() == item).mapToInt(PayLine::amount).sum();
    }

    @Test
    void 설계도_예시_5인_이상이면_합계_1025808원() {
        records.saveContract(contract(BusinessSize.FIVE_OR_MORE));
        workAllContractDays(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        PayrollResult result = service.calculate(NOV);

        assertEquals(NOV, result.month());
        assertEquals(4_590, result.totalWorkMinutes());
        assertEquals(789_480, amountOf(result, PayItem.BASE));
        assertEquals(148_608, amountOf(result, PayItem.WEEKLY_HOLIDAY));
        assertEquals(87_720, amountOf(result, PayItem.NIGHT));
        assertEquals(0, amountOf(result, PayItem.PART_TIME_OVERTIME));
        assertEquals(0, amountOf(result, PayItem.OVERTIME));
        assertEquals(0, amountOf(result, PayItem.HOLIDAY));
        assertEquals(0, amountOf(result, PayItem.SHUTDOWN_ALLOWANCE));
        assertEquals(1_025_808, result.total());
        assertEquals(result.total(), result.lines().stream().mapToInt(PayLine::amount).sum());
    }

    @Test
    void 같은_조건_5인_미만이면_야간수당이_빠져_938088원() {
        records.saveContract(contract(BusinessSize.UNDER_FIVE));
        workAllContractDays(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        PayrollResult result = service.calculate(NOV);

        assertEquals(0, amountOf(result, PayItem.NIGHT));
        assertEquals(938_088, result.total());
        assertEquals(List.of(PayItem.BASE, PayItem.WEEKLY_HOLIDAY),
                result.lines().stream().map(PayLine::item).distinct().toList());
    }

    @Test
    void 근로_조건이_없으면_예외() {
        assertThrows(ValidationException.class, () -> service.calculate(NOV));
    }

    @Test
    void 근무_기록이_없으면_합계_0인_결과() {
        records.saveContract(contract(BusinessSize.FIVE_OR_MORE));

        PayrollResult result = service.calculate(NOV);

        assertEquals(0, result.total());
        assertEquals(0, result.totalWorkMinutes());
        assertTrue(result.lines().isEmpty());
    }

    @Test
    void 월초에_걸친_주는_지난달_기록으로_개근을_판단해_이번_달에_계산() {
        records.saveContract(contract(BusinessSize.FIVE_OR_MORE));
        // 11/30(월)은 11월 기록, 12/2(수)·12/4(금)·12/5(토)는 12월 기록. 일요일(12/6)이 12월이므로 12월 주휴수당
        work(LocalDate.of(2026, 11, 30));
        work(LocalDate.of(2026, 12, 2));
        work(LocalDate.of(2026, 12, 4));
        work(LocalDate.of(2026, 12, 5));

        PayrollResult dec = service.calculate(DEC);
        PayrollResult nov = service.calculate(NOV);

        assertEquals(37_152, amountOf(dec, PayItem.WEEKLY_HOLIDAY));
        assertTrue(dec.lines().stream().anyMatch(l -> l.description().startsWith("11/30~12/6")));
        assertEquals(810, dec.totalWorkMinutes()); // 12월 기록 3일만. 11/30은 기본급에 넣지 않는다
        assertEquals(139_320, amountOf(dec, PayItem.BASE));
        assertEquals(0, amountOf(nov, PayItem.WEEKLY_HOLIDAY));
    }

    @Test
    void 급여_계산은_연도_제한이_없다() {
        records.saveContract(contract(BusinessSize.FIVE_OR_MORE));
        // 최저임금 표(2025~2027)에 없는 연도의 달도 계산된다. 2028-01-03은 월요일
        work(LocalDate.of(2028, 1, 3));

        PayrollResult result = service.calculate(YearMonth.of(2028, 1));

        assertEquals(270, result.totalWorkMinutes());
        assertEquals(46_440, amountOf(result, PayItem.BASE)); // 270분 × 10,320 ÷ 60
        assertEquals(0, service.calculate(YearMonth.of(2024, 12)).total());
    }
}
