package albaguard.engine.rule;

import albaguard.domain.BusinessSize;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NightPayRuleTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final int WAGE = 10_320;

    private static Contract contract(int wage) {
        return new Contract("편의점", wage, LocalDate.of(2026, 9, 1), null,
                Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), LocalTime.of(23, 0), 30,
                false, null, BusinessSize.FIVE_OR_MORE, true);
    }

    private static WorkRecord record(int day, String in, String out, int breakMinutes, boolean holiday) {
        return new WorkRecord("r" + day, LocalDate.of(2026, 11, day), LocalTime.parse(in),
                LocalTime.parse(out), breakMinutes, holiday, EarlyLeaveReason.NONE, "");
    }

    private static List<PayLine> apply(int wage, List<WorkRecord> records) {
        return new NightPayRule().apply(new PayContext(contract(wage), NOV, records));
    }

    @Test
    void 저녁_18시_23시_17일이면_87720원() {
        List<WorkRecord> records = new ArrayList<>();
        for (int day = 1; day <= 17; day++) {
            records.add(record(day, "18:00", "23:00", 30, false));
        }

        List<PayLine> lines = apply(WAGE, records);

        assertEquals(1, lines.size());
        assertEquals(PayItem.NIGHT, lines.get(0).item());
        assertEquals(87_720, lines.get(0).amount());
        assertEquals("야간 17시간 × 10,320원 × 50%", lines.get(0).description());
    }

    @Test
    void 자정을_넘는_22시_02시_하루면_20640원() {
        assertEquals(20_640, apply(WAGE, List.of(record(2, "22:00", "02:00", 0, false))).get(0).amount());
    }

    @Test
    void 새벽_출근은_6시까지만_야간으로_계산() {
        // 05:00~09:00 → 야간 60분 → 5,160원
        assertEquals(5_160, apply(WAGE, List.of(record(2, "05:00", "09:00", 0, false))).get(0).amount());
    }

    @Test
    void 야간_근무가_없으면_PayLine_없음() {
        assertTrue(apply(WAGE, List.of(record(2, "09:00", "18:00", 60, false))).isEmpty());
        assertTrue(apply(WAGE, List.of()).isEmpty());
    }

    @Test
    void 휴일_근무의_야간_시간도_별도로_가산() {
        assertEquals(20_640, apply(WAGE, List.of(record(2, "22:00", "02:00", 0, true))).get(0).amount());
    }

    @Test
    void 원_단위는_0점5_이상_올림() {
        // 1분 × 120원 ÷ 120 = 1원 / 1분 × 59원 ÷ 120 = 0.49원 → 0원 / 1분 × 60원 ÷ 120 = 0.5원 → 1원
        List<WorkRecord> oneMinute = List.of(record(2, "22:00", "22:01", 0, false));
        assertEquals(1, apply(120, oneMinute).get(0).amount());
        assertEquals(1, apply(60, oneMinute).get(0).amount());
        assertEquals(0, apply(59, oneMinute).get(0).amount());
    }
}
