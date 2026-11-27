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

class BasePayRuleTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);

    private static Contract contract(int wage) {
        return new Contract("편의점", wage, LocalDate.of(2026, 9, 1), null,
                Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), LocalTime.of(23, 0), 30,
                false, null, BusinessSize.FIVE_OR_MORE, true);
    }

    private static WorkRecord record(int day, String in, String out, int breakMinutes) {
        return new WorkRecord("r" + day, LocalDate.of(2026, 11, day), LocalTime.parse(in),
                LocalTime.parse(out), breakMinutes, false, EarlyLeaveReason.NONE, "");
    }

    @Test
    void 근무_4590분_시급_10320원이면_789480원() {
        List<WorkRecord> records = new ArrayList<>();
        for (int day = 1; day <= 17; day++) { // 18:00~23:00, 휴게 30분 = 270분 × 17일
            records.add(record(day, "18:00", "23:00", 30));
        }

        List<PayLine> lines = new BasePayRule().apply(new PayContext(contract(10_320), NOV, records));

        assertEquals(1, lines.size());
        assertEquals(PayItem.BASE, lines.get(0).item());
        assertEquals(789_480, lines.get(0).amount());
        assertEquals("76시간 30분 × 10,320원", lines.get(0).description());
    }

    @Test
    void 근무_기록이_없으면_PayLine_없음() {
        List<PayLine> lines = new BasePayRule().apply(new PayContext(contract(10_320), NOV, List.of()));
        assertTrue(lines.isEmpty());
    }

    @Test
    void 원_단위는_0점5_이상_올림() {
        // 1분 × 30원 ÷ 60 = 0.5 → 1원, 1분 × 29원 ÷ 60 = 0.48 → 0원
        PayContext up = new PayContext(contract(30), NOV, List.of(record(2, "18:00", "18:01", 0)));
        PayContext down = new PayContext(contract(29), NOV, List.of(record(2, "18:00", "18:01", 0)));

        assertEquals(1, new BasePayRule().apply(up).get(0).amount());
        assertEquals(0, new BasePayRule().apply(down).get(0).amount());
    }

    @Test
    void 자정을_넘는_근무도_합산() {
        List<WorkRecord> records = List.of(record(2, "22:00", "02:00", 0)); // 240분
        List<PayLine> lines = new BasePayRule().apply(new PayContext(contract(10_320), NOV, records));

        assertEquals(41_280, lines.get(0).amount()); // 4시간 × 10,320
        assertEquals("4시간 × 10,320원", lines.get(0).description());
    }
}
