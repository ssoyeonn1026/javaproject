package albaguard.engine.rule;

import albaguard.domain.Contract;
import albaguard.domain.EarlyLeaveReason;
import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.domain.WorkRecord;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static albaguard.engine.rule.OvertimeTestSupport.WAGE;
import static albaguard.engine.rule.OvertimeTestSupport.contract;
import static albaguard.engine.rule.OvertimeTestSupport.mondayEvening;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownAllowanceRuleTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);

    private static WorkRecord record(int day, String in, String out, EarlyLeaveReason reason) {
        return new WorkRecord("r" + day, LocalDate.of(2026, 11, day), LocalTime.parse(in), LocalTime.parse(out),
                0, false, reason, "");
    }

    private static List<PayLine> apply(Contract c, List<WorkRecord> records) {
        return new ShutdownAllowanceRule().apply(new PayContext(c, NOV, records));
    }

    @Test
    void 계약_23시_고용주_지시로_21시_퇴근하면_14448원() {
        List<PayLine> lines = apply(mondayEvening(),
                List.of(record(2, "18:00", "21:00", EarlyLeaveReason.EMPLOYER_ORDER)));

        assertEquals(1, lines.size());
        assertEquals(PayItem.SHUTDOWN_ALLOWANCE, lines.get(0).item());
        assertEquals(14_448, lines.get(0).amount()); // 2시간 × 10,320 × 0.7
        assertEquals("휴업 2시간 × 10,320원 × 70%", lines.get(0).description());
    }

    @Test
    void 본인_사정_조기_퇴근은_0원() {
        assertTrue(apply(mondayEvening(),
                List.of(record(2, "18:00", "21:00", EarlyLeaveReason.PERSONAL))).isEmpty());
    }

    @Test
    void 조기_퇴근이_아니면_0원() {
        assertTrue(apply(mondayEvening(), List.of(record(2, "18:00", "23:00", EarlyLeaveReason.NONE))).isEmpty());
        assertTrue(apply(mondayEvening(), List.of()).isEmpty());
    }

    @Test
    void 계약_퇴근_이후에_퇴근하면_못_일한_시간이_없어_0원() {
        assertTrue(apply(mondayEvening(),
                List.of(record(2, "18:00", "23:30", EarlyLeaveReason.EMPLOYER_ORDER))).isEmpty());
    }

    @Test
    void 자정을_넘는_계약도_계산() {
        // 계약 22:00~02:00, 고용주 지시로 00:30 퇴근 → 90분 × 10,320 × 0.7 ÷ 60 = 10,836원
        Contract overnight = contract(Set.of(DayOfWeek.MONDAY), LocalTime.of(22, 0), LocalTime.of(2, 0), 0);
        List<PayLine> lines = apply(overnight,
                List.of(record(2, "22:00", "00:30", EarlyLeaveReason.EMPLOYER_ORDER)));
        assertEquals(10_836, lines.get(0).amount());
    }

    @Test
    void 계약_요일이_아닌_날_고용주_지시_조기_퇴근은_0원() {
        // mondayEvening은 월요일만 근무 계약. 11/3은 화요일
        assertTrue(apply(mondayEvening(),
                List.of(record(3, "18:00", "21:00", EarlyLeaveReason.EMPLOYER_ORDER))).isEmpty());
    }

    @Test
    void 계약_기간_밖의_날짜는_0원() {
        Contract c = mondayEvening();
        Contract ended = new Contract(c.workplaceName(), c.hourlyWage(), c.startDate(), LocalDate.of(2026, 11, 8),
                c.workDays(), c.scheduledStart(), c.scheduledEnd(), c.scheduledBreakMinutes(), false, null,
                c.businessSize(), true);
        Contract notStarted = new Contract(c.workplaceName(), c.hourlyWage(), LocalDate.of(2026, 11, 3), null,
                c.workDays(), c.scheduledStart(), c.scheduledEnd(), c.scheduledBreakMinutes(), false, null,
                c.businessSize(), true);

        // 종료일(11/8) 이후의 월요일(11/9), 시작일(11/3) 이전의 월요일(11/2)은 계산하지 않는다
        assertTrue(apply(ended, List.of(record(9, "18:00", "21:00", EarlyLeaveReason.EMPLOYER_ORDER))).isEmpty());
        assertTrue(apply(notStarted, List.of(record(2, "18:00", "21:00", EarlyLeaveReason.EMPLOYER_ORDER))).isEmpty());

        // 계약 기간 안의 월요일(11/2)은 계산한다
        assertEquals(14_448, apply(ended,
                List.of(record(2, "18:00", "21:00", EarlyLeaveReason.EMPLOYER_ORDER))).get(0).amount());
    }

    @Test
    void 소정근로일이_아닌_날은_빼고_소정근로일만_합산() {
        List<PayLine> lines = apply(mondayEvening(), List.of(
                record(2, "18:00", "21:00", EarlyLeaveReason.EMPLOYER_ORDER),
                record(3, "18:00", "21:00", EarlyLeaveReason.EMPLOYER_ORDER)));
        assertEquals(14_448, lines.get(0).amount());
    }

    @Test
    void 여러_날의_휴업_시간을_합산() {
        List<PayLine> lines = apply(mondayEvening(), List.of(
                record(2, "18:00", "21:00", EarlyLeaveReason.EMPLOYER_ORDER),
                record(9, "18:00", "22:00", EarlyLeaveReason.EMPLOYER_ORDER),
                record(16, "18:00", "20:00", EarlyLeaveReason.PERSONAL)));

        assertEquals(21_672, lines.get(0).amount()); // (120 + 60)분 = 3시간 × 10,320 × 0.7
        assertEquals("휴업 3시간 × 10,320원 × 70%", lines.get(0).description());
    }

    @Test
    void 원_단위는_0점5_이상_올림() {
        // 1분 × 10,320 × 0.7 ÷ 60 = 120.4원 → 120원
        List<PayLine> lines = apply(mondayEvening(),
                List.of(record(2, "18:00", "22:59", EarlyLeaveReason.EMPLOYER_ORDER)));
        assertEquals(120, lines.get(0).amount());
    }
}
