package albaguard.engine.rule;

import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.engine.WorkTime;

import java.util.List;

/**
 * R6 야간근로수당: 야간(22시~다음 날 6시) 근무 분 × 시급 ÷ 60 × 0.5 (원 단위 반올림).
 * 연장·휴일 가산과 별도로 더하므로 휴일 근무 기록의 야간 시간도 포함한다.
 * 5인 이상 사업장에만 적용하며, 규칙을 고르는 일은 RuleSets(T13)가 한다.
 */
public class NightPayRule implements PayRule {

    @Override
    public List<PayLine> apply(PayContext ctx) {
        int nightMinutes = ctx.records().stream().mapToInt(WorkTime::nightMinutes).sum();
        if (nightMinutes == 0) {
            return List.of();
        }
        int wage = ctx.contract().hourlyWage();
        // 분 × 시급 ÷ 60 × 0.5 = 분 × 시급 ÷ 120 을 정수로 계산해 0.5 이상 올림
        int amount = (int) (((long) nightMinutes * wage + 60) / 120);
        String description = String.format("야간 %s × %,d원 × 50%%", PayFormat.duration(nightMinutes), wage);
        return List.of(new PayLine(PayItem.NIGHT, amount, description));
    }
}
