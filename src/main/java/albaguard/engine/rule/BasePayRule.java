package albaguard.engine.rule;

import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.engine.WorkTime;

import java.util.List;

/**
 * R1 기본급: 그 달 실근로 분 합계 × 시급 ÷ 60 (원 단위 반올림).
 * 근무 기록이 없거나 근무 분이 0이면 PayLine을 만들지 않는다.
 */
public class BasePayRule implements PayRule {

    @Override
    public List<PayLine> apply(PayContext ctx) {
        int minutes = ctx.records().stream().mapToInt(WorkTime::workMinutes).sum();
        if (minutes == 0) {
            return List.of();
        }
        int wage = ctx.contract().hourlyWage();
        // 분 × 시급 ÷ 60 을 정수로 계산해 0.5 이상 올림 (부동소수점 오차 방지)
        int amount = (int) (((long) minutes * wage * 2 + 60) / 120);
        String description = String.format("%s × %,d원", formatDuration(minutes), wage);
        return List.of(new PayLine(PayItem.BASE, amount, description));
    }

    /** 분을 "76시간 30분" 형태로 바꾼다. 분이 0이면 "76시간". */
    private static String formatDuration(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (rest == 0) {
            return hours + "시간";
        }
        return hours == 0 ? rest + "분" : hours + "시간 " + rest + "분";
    }
}
