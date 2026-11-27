package albaguard.engine.rule;

import albaguard.domain.Contract;
import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.domain.WorkRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static albaguard.engine.rule.OvertimeTestSupport.WAGE;
import static albaguard.engine.rule.OvertimeTestSupport.fullTime;
import static albaguard.engine.rule.OvertimeTestSupport.mondayEvening;
import static albaguard.engine.rule.OvertimeTestSupport.record;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvertimePayRuleTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);
    private static final LocalDate MON = LocalDate.of(2026, 11, 2);

    private static List<PayLine> apply(Contract c, List<WorkRecord> records) {
        return new OvertimePayRule().apply(new PayContext(c, NOV, records));
    }

    /** 월요일(11/2)부터 days일 연속, 하루 hours시간 근무 (휴게 0). */
    private static List<WorkRecord> days(int days, int hours) {
        List<WorkRecord> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            list.add(record(MON.plusDays(i), "09:00", String.format("%02d:00", 9 + hours), 0));
        }
        return list;
    }

    @Test
    void 하루_9시간이면_1시간_연장() {
        List<PayLine> lines = apply(fullTime(), List.of(record(MON, "09:00", "18:00", 0)));

        assertEquals(1, lines.size());
        assertEquals(PayItem.OVERTIME, lines.get(0).item());
        assertEquals(5_160, lines.get(0).amount()); // 60분 × 10,320 ÷ 120
        assertEquals("연장 1시간(하루 초과 1시간 + 주 초과 0시간) × 10,320원 × 50%", lines.get(0).description());
    }

    @Test
    void 하루_8시간_6일_주_48시간이면_8시간_연장_두_번_세지_않음() {
        List<PayLine> lines = apply(fullTime(), days(6, 8));

        assertEquals(41_280, lines.get(0).amount()); // 480분 × 10,320 ÷ 120
        assertEquals("연장 8시간(하루 초과 0시간 + 주 초과 8시간) × 10,320원 × 50%", lines.get(0).description());
    }

    @Test
    void 하루_10시간_5일_주_50시간이면_하루_초과_10시간만_주_초과는_추가_없음() {
        List<PayLine> lines = apply(fullTime(), days(5, 10));

        assertEquals(51_600, lines.get(0).amount()); // 600분 × 10,320 ÷ 120
        assertEquals("연장 10시간(하루 초과 10시간 + 주 초과 0시간) × 10,320원 × 50%", lines.get(0).description());
    }

    @Test
    void 하루_8시간_5일_주_40시간이면_연장_없음() {
        assertTrue(apply(fullTime(), days(5, 8)).isEmpty());
    }

    @Test
    void 휴일_근무_기록은_연장_계산에서_제외() {
        assertTrue(apply(fullTime(), List.of(record("h", MON, "09:00", "20:00", 0, true))).isEmpty());

        // 휴일 기록은 주 합계에도 들어가지 않는다: 평일 5일 40시간 + 휴일 8시간 → 연장 없음
        List<WorkRecord> records = days(5, 8);
        records.add(record("h", MON.plusDays(5), "09:00", "17:00", 0, true));
        assertTrue(apply(fullTime(), records).isEmpty());
    }

    @Test
    void 단시간근로자의_초과근로와_연장이_같은_시간을_두_번_세지_않음() {
        // 계약 하루 270분. 12시간(720분) 근무 → R3 초과 210분(8시간 이내), R4 하루 연장 240분 → 합 450 = 720 - 270
        List<WorkRecord> records = List.of(record(MON, "08:00", "20:00", 0));

        assertEquals(240 * WAGE / 120, apply(mondayEvening(), records).get(0).amount());
        assertEquals(210 * WAGE / 120,
                new PartTimeOvertimeRule().apply(new PayContext(mondayEvening(), NOV, records))
                        .get(0).amount());
    }

    @Test
    void 월말에_걸친_주의_주_연장은_일요일이_속한_달에_계산() {
        // 11/30(월)~12/5(토) 하루 8시간 6일 = 48시간. 일요일(12/6)이 12월이므로 12월에만 주 연장 480분
        WorkRecord nov30 = record("nov30", LocalDate.of(2026, 11, 30), "09:00", "17:00", 0, false);
        List<WorkRecord> decRecords = new ArrayList<>();
        for (int d = 1; d <= 5; d++) {
            decRecords.add(record(LocalDate.of(2026, 12, d), "09:00", "17:00", 0));
        }

        List<PayLine> novLines = new OvertimePayRule()
                .apply(new PayContext(fullTime(), NOV, List.of(nov30), decRecords));
        List<PayLine> decLines = new OvertimePayRule()
                .apply(new PayContext(fullTime(), DEC, decRecords, List.of(nov30)));

        assertTrue(novLines.isEmpty());
        assertEquals(41_280, decLines.get(0).amount());
    }

    @Test
    void 근무_기록이_없으면_PayLine_없음() {
        assertTrue(apply(fullTime(), List.of()).isEmpty());
    }
}
