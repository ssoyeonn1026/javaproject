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
import static albaguard.engine.rule.OvertimeTestSupport.record;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HolidayPayRuleTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final LocalDate SUN = LocalDate.of(2026, 11, 1);

    private static List<PayLine> apply(Contract c, List<WorkRecord> records) {
        return new HolidayPayRule().apply(new PayContext(c, NOV, records, WAGE));
    }

    private static WorkRecord holiday(String id, String in, String out) {
        return record(id, SUN, in, out, 0, true);
    }

    @Test
    void 휴일_10시간_근무는_8시간_50퍼센트_더하기_2시간_100퍼센트() {
        List<PayLine> lines = apply(fullTime(), List.of(holiday("h", "09:00", "19:00")));

        assertEquals(1, lines.size());
        assertEquals(PayItem.HOLIDAY, lines.get(0).item());
        assertEquals(61_920, lines.get(0).amount()); // 8시간 × 5,160 + 2시간 × 10,320
        assertEquals("휴일 8시간 × 50% + 2시간 × 100% (시급 10,320원)", lines.get(0).description());
    }

    @Test
    void 휴일_8시간_이내는_50퍼센트만() {
        List<PayLine> lines = apply(fullTime(), List.of(holiday("h", "09:00", "17:00")));

        assertEquals(41_280, lines.get(0).amount());
        assertEquals("휴일 8시간 × 50% (시급 10,320원)", lines.get(0).description());
    }

    @Test
    void 같은_날_휴일_기록_여러_건은_합쳐서_8시간_기준으로_계산() {
        // 5시간 + 5시간 = 10시간
        List<PayLine> lines = apply(fullTime(),
                List.of(holiday("a", "09:00", "14:00"), holiday("b", "15:00", "20:00")));
        assertEquals(61_920, lines.get(0).amount());
    }

    @Test
    void 날짜가_다르면_날마다_8시간_기준으로_계산() {
        // 휴일 5시간 두 날: 각각 8시간 이내 → 10시간 × 5,160 = 51,600
        List<PayLine> lines = apply(fullTime(), List.of(
                record("a", SUN, "09:00", "14:00", 0, true),
                record("b", LocalDate.of(2026, 11, 8), "09:00", "14:00", 0, true)));
        assertEquals(51_600, lines.get(0).amount());
    }

    @Test
    void 휴일이_아닌_근무는_제외() {
        assertTrue(apply(fullTime(), List.of(record(SUN, "09:00", "19:00", 0))).isEmpty());
        assertTrue(apply(fullTime(), List.of()).isEmpty());
    }

    @Test
    void 휴게시간은_근무시간에서_제외() {
        // 09:00~19:00, 휴게 60분 → 9시간 → 8시간 × 5,160 + 1시간 × 10,320 = 51,600
        List<PayLine> lines = apply(fullTime(), List.of(record("h", SUN, "09:00", "19:00", 60, true)));
        assertEquals(51_600, lines.get(0).amount());
    }
}
