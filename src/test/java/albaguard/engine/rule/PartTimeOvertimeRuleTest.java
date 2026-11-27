package albaguard.engine.rule;

import albaguard.domain.Contract;
import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.domain.WorkRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static albaguard.engine.rule.OvertimeTestSupport.WAGE;
import static albaguard.engine.rule.OvertimeTestSupport.fullTime;
import static albaguard.engine.rule.OvertimeTestSupport.mondayEvening;
import static albaguard.engine.rule.OvertimeTestSupport.record;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartTimeOvertimeRuleTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final LocalDate MON = LocalDate.of(2026, 11, 2);
    private static final LocalDate TUE = LocalDate.of(2026, 11, 3);

    private static List<PayLine> apply(Contract c, List<WorkRecord> records) {
        return new PartTimeOvertimeRule().apply(new PayContext(c, NOV, records, WAGE));
    }

    @Test
    void 계약_23시인데_24시_퇴근하면_초과_60분_5160원() {
        List<PayLine> lines = apply(mondayEvening(), List.of(record(MON, "18:00", "00:00", 30)));

        assertEquals(1, lines.size());
        assertEquals(PayItem.PART_TIME_OVERTIME, lines.get(0).item());
        assertEquals(5_160, lines.get(0).amount());
        assertEquals("초과근로 1시간 × 10,320원 × 50%", lines.get(0).description());
    }

    @Test
    void 계약_요일이_아닌_날_5시간_대타는_초과_300분() {
        List<PayLine> lines = apply(mondayEvening(), List.of(record(TUE, "10:00", "15:00", 0)));

        assertEquals(25_800, lines.get(0).amount()); // 300분 × 10,320 ÷ 120
        assertEquals("초과근로 5시간 × 10,320원 × 50%", lines.get(0).description());
    }

    @Test
    void 주_40시간_계약자는_적용_안_됨() {
        assertTrue(apply(fullTime(), List.of(record(MON, "09:00", "20:00", 60))).isEmpty());
    }

    @Test
    void 계약_시간만큼_또는_덜_일하면_초과_없음() {
        assertTrue(apply(mondayEvening(), List.of(record(MON, "18:00", "23:00", 30))).isEmpty());
        assertTrue(apply(mondayEvening(), List.of(record(MON, "18:00", "21:00", 0))).isEmpty());
    }

    @Test
    void 하루_8시간을_넘는_부분은_연장근로_몫이라_8시간까지만_계산() {
        // 10시간 근무: min(600, 480) - 270 = 210분
        List<PayLine> lines = apply(mondayEvening(), List.of(record(MON, "09:00", "19:00", 0)));
        assertEquals(210 * WAGE / 120, lines.get(0).amount());
    }

    @Test
    void 휴일_근무_기록은_제외() {
        assertTrue(apply(mondayEvening(),
                List.of(record("h", MON, "18:00", "00:00", 30, true))).isEmpty());
    }

    @Test
    void 같은_날_여러_건은_합쳐서_하루_기준으로_계산() {
        // 12:00~14:00(120분) + 18:00~23:30(휴게 30분, 300분) = 420분 → 420 - 270 = 150분
        List<PayLine> lines = apply(mondayEvening(),
                List.of(record(MON, "12:00", "14:00", 0), record(MON, "18:00", "23:30", 30)));
        assertEquals(150 * WAGE / 120, lines.get(0).amount());
    }

    @Test
    void 근무_기록이_없으면_PayLine_없음() {
        assertTrue(apply(mondayEvening(), List.of()).isEmpty());
    }
}
