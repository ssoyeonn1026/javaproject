package albaguard.engine.rule;

import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.domain.WorkRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * R4 연장근로수당 (근로기준법 제50조, 제56조 제1항).
 * <pre>
 * 하루 연장 분 = max(0, 실근로 분 - 480)
 * 주 연장 분   = max(0, 그 주 실근로 분 합계 - 2,400 - 그 주 하루 연장 분 합계 - 그 주 R3 초과근로 분 합계)
 * 수당         = (하루 연장 분 합계 + 주 연장 분) × 시급 ÷ 60 × 0.5
 * </pre>
 * 같은 시간을 하루 기준, 주 기준, R3에서 두 번 세지 않는다. 휴일 근무 기록은 제외한다.
 * 하루 연장은 이번 달 기록의 날짜별로 세고, 주 연장은 월~일 한 주를 그 주 일요일이 속한 달에 계산한다.
 * 이때 다른 달에 걸친 날은 {@link PayContext#adjacentRecords()}로 확인한다.
 * 5인 이상 사업장에만 적용하며, 규칙을 고르는 일은 RuleSets(T13)가 한다.
 */
public class OvertimePayRule implements PayRule {

    @Override
    public List<PayLine> apply(PayContext ctx) {
        int dailyMinutes = 0;
        for (int actual : OvertimeCalc.actualByDate(ctx.records()).values()) {
            dailyMinutes += OvertimeCalc.dailyOvertime(actual);
        }

        // 주 기준은 월 경계에 걸친 주를 위해 이웃 달 기록까지 합쳐서 계산 (같은 id는 한 번만)
        Map<LocalDate, Integer> actualByDate = OvertimeCalc.actualByDate(withAdjacent(ctx));
        int weeklyMinutes = 0;
        for (LocalDate sunday : Weeks.sundaysOf(ctx.month())) {
            weeklyMinutes += weeklyOvertime(ctx, actualByDate, sunday);
        }

        int overtimeMinutes = dailyMinutes + weeklyMinutes;
        if (overtimeMinutes == 0) {
            return List.of();
        }
        int wage = ctx.contract().hourlyWage();
        // 분 × 시급 ÷ 60 × 0.5 = 분 × 시급 ÷ 120 을 정수로 계산해 0.5 이상 올림
        int amount = (int) (((long) overtimeMinutes * wage + 60) / 120);
        String description = String.format("연장 %s(하루 초과 %s + 주 초과 %s) × %,d원 × 50%%",
                PayFormat.duration(overtimeMinutes), PayFormat.duration(dailyMinutes),
                PayFormat.duration(weeklyMinutes), wage);
        return List.of(new PayLine(PayItem.OVERTIME, amount, description));
    }

    private static int weeklyOvertime(PayContext ctx, Map<LocalDate, Integer> actualByDate, LocalDate sunday) {
        int weekTotal = 0;
        int weekDaily = 0;
        int weekPartTime = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate date = sunday.minusDays(i);
            int actual = actualByDate.getOrDefault(date, 0);
            weekTotal += actual;
            weekDaily += OvertimeCalc.dailyOvertime(actual);
            weekPartTime += OvertimeCalc.partTimeExcess(ctx.contract(), date, actual);
        }
        return Math.max(0, weekTotal - OvertimeCalc.WEEK_LIMIT - weekDaily - weekPartTime);
    }

    private static List<WorkRecord> withAdjacent(PayContext ctx) {
        List<WorkRecord> all = new ArrayList<>(ctx.records());
        Set<String> ids = new HashSet<>();
        ctx.records().forEach(r -> ids.add(r.id()));
        for (WorkRecord r : ctx.adjacentRecords()) {
            if (ids.add(r.id())) {
                all.add(r);
            }
        }
        return all;
    }
}
