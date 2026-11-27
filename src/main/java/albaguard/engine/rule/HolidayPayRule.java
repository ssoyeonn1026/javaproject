package albaguard.engine.rule;

import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.domain.WorkRecord;
import albaguard.engine.WorkTime;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * R5 휴일근로수당 (근로기준법 제56조 제2항).
 * <pre>
 * 휴일근로수당 = min(실근로 분, 480) × 시급 ÷ 60 × 0.5
 *             + max(0, 실근로 분 - 480) × 시급 ÷ 60 × 1.0
 * </pre>
 * {@code holiday = true}인 근무 기록만 대상이며, 같은 날 여러 건은 합쳐서 하루 기준으로 계산한다.
 * 휴일 근무는 초과근로(R3)와 연장근로(R4)에 포함하지 않는다. 야간 가산은 NightPayRule이 따로 더한다.
 * 5인 이상 사업장에만 적용하며, 규칙을 고르는 일은 RuleSets(T13)가 한다.
 */
public class HolidayPayRule implements PayRule {

    @Override
    public List<PayLine> apply(PayContext ctx) {
        Map<LocalDate, Integer> minutesByDate = new TreeMap<>();
        for (WorkRecord r : ctx.records()) {
            if (r.holiday()) {
                minutesByDate.merge(r.date(), WorkTime.workMinutes(r), Integer::sum);
            }
        }

        int basicMinutes = 0; // 8시간 이내, 50% 가산
        int extraMinutes = 0; // 8시간 초과, 100% 가산
        for (int minutes : minutesByDate.values()) {
            basicMinutes += Math.min(minutes, OvertimeCalc.DAY_LIMIT);
            extraMinutes += Math.max(0, minutes - OvertimeCalc.DAY_LIMIT);
        }
        if (basicMinutes + extraMinutes == 0) {
            return List.of();
        }

        int wage = ctx.contract().hourlyWage();
        // (기본 분 × 0.5 + 초과 분 × 1.0) × 시급 ÷ 60 = (기본 분 + 초과 분 × 2) × 시급 ÷ 120 을 0.5 이상 올림
        long weightedMinutes = basicMinutes + 2L * extraMinutes;
        int amount = (int) ((weightedMinutes * wage + 60) / 120);

        String description = String.format("휴일 %s × 50%%", PayFormat.duration(basicMinutes));
        if (extraMinutes > 0) {
            description += String.format(" + %s × 100%%", PayFormat.duration(extraMinutes));
        }
        description += String.format(" (시급 %,d원)", wage);
        return List.of(new PayLine(PayItem.HOLIDAY, amount, description));
    }
}
