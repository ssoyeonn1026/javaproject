package albaguard.engine.rule;

import albaguard.domain.PayItem;
import albaguard.domain.PayLine;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * R3 단시간근로자 초과근로수당 (기간제법 제6조 제3항).
 * 1주 소정근로시간이 40시간 미만인 근로자가 그날 소정근로시간을 넘겨 일한 시간(하루 8시간 이내)에
 * 통상임금의 50%를 가산한다. 주 40시간을 넘지 않아도 적용된다.
 * 8시간을 넘는 부분은 연장근로(OvertimePayRule)가 계산하고, 휴일 근무 기록은 제외한다.
 * 5인 이상 사업장에만 적용하며, 규칙을 고르는 일은 RuleSets(T13)가 한다.
 */
public class PartTimeOvertimeRule implements PayRule {

    @Override
    public List<PayLine> apply(PayContext ctx) {
        int excessMinutes = 0;
        for (Map.Entry<LocalDate, Integer> day : OvertimeCalc.actualByDate(ctx.records()).entrySet()) {
            excessMinutes += OvertimeCalc.partTimeExcess(ctx.contract(), day.getKey(), day.getValue());
        }
        if (excessMinutes == 0) {
            return List.of();
        }
        int wage = ctx.contract().hourlyWage();
        // 분 × 시급 ÷ 60 × 0.5 = 분 × 시급 ÷ 120 을 정수로 계산해 0.5 이상 올림
        int amount = (int) (((long) excessMinutes * wage + 60) / 120);
        String description = String.format("초과근로 %s × %,d원 × 50%%", PayFormat.duration(excessMinutes), wage);
        return List.of(new PayLine(PayItem.PART_TIME_OVERTIME, amount, description));
    }
}
