package albaguard.engine.rule;

import albaguard.domain.Contract;
import albaguard.domain.EarlyLeaveReason;
import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.domain.WorkRecord;

import java.time.LocalTime;
import java.util.List;

/**
 * R7 휴업수당 (근로기준법 제46조, 시행령 제26조).
 * <pre>
 * 못 일한 분 = 계약 퇴근 시각 - 실제 퇴근 시각   ({@code earlyLeaveReason = EMPLOYER_ORDER}인 기록만)
 * 휴업수당   = 못 일한 분 × 시급 ÷ 60 × 0.7
 * </pre>
 * 계약 근무 요일이면서 계약 기간 안인 날(소정근로일, 주휴수당과 같은 기준)의 기록만 계산한다.
 * 평균임금 대신 시급으로 근사한다. 본인 사정 조기 퇴근(PERSONAL)은 대상이 아니다.
 * 계약 퇴근 시각이 자정을 넘어도 계산하도록 계약 출근 시각을 기준으로 시각을 비교한다.
 * 5인 이상 사업장에만 적용하며, 규칙을 고르는 일은 RuleSets(T13)가 한다.
 */
public class ShutdownAllowanceRule implements PayRule {

    private static final int DAY_MINUTES = 24 * 60;

    @Override
    public List<PayLine> apply(PayContext ctx) {
        Contract contract = ctx.contract();
        int missedMinutes = 0;
        for (WorkRecord r : ctx.records()) {
            if (r.earlyLeaveReason() == EarlyLeaveReason.EMPLOYER_ORDER
                    && ScheduledDays.isScheduled(contract, r.date())) {
                missedMinutes += missedMinutes(contract, r);
            }
        }
        if (missedMinutes == 0) {
            return List.of();
        }

        int wage = contract.hourlyWage();
        // 분 × 시급 ÷ 60 × 0.7 = 분 × 시급 × 7 ÷ 600 을 정수로 계산해 0.5 이상 올림
        int amount = (int) (((long) missedMinutes * wage * 7 + 300) / 600);
        String description = String.format("휴업 %s × %,d원 × 70%%", PayFormat.duration(missedMinutes), wage);
        return List.of(new PayLine(PayItem.SHUTDOWN_ALLOWANCE, amount, description));
    }

    /** 계약 퇴근 시각보다 일찍 퇴근한 분. 계약 시간보다 늦게 퇴근했으면 0. */
    private static int missedMinutes(Contract contract, WorkRecord record) {
        LocalTime start = contract.scheduledStart();
        int scheduledEnd = minutesSince(start, contract.scheduledEnd());
        int actualEnd = minutesSince(start, record.clockOut());
        return Math.max(0, scheduledEnd - actualEnd);
    }

    /** 기준 시각에서 time까지 흐른 분(0~1,439). 자정을 넘으면 다음 날로 본다. */
    private static int minutesSince(LocalTime base, LocalTime time) {
        int diff = (time.getHour() * 60 + time.getMinute()) - (base.getHour() * 60 + base.getMinute());
        return diff < 0 ? diff + DAY_MINUTES : diff;
    }
}
