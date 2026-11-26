package albaguard.engine.rule;

import albaguard.domain.Contract;
import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.domain.WorkRecord;
import albaguard.engine.WorkTime;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * R2 주휴수당 (docs/CALC_RULES.md).
 * <ul>
 *   <li>지급 조건: 1주 소정근로시간 15시간 이상, 그 주의 소정근로일(계약 근무 요일)에 모두 출근</li>
 *   <li>계산: (1주 소정근로시간 ÷ 40) × 8시간 × 시급, 최대 8시간분</li>
 *   <li>주는 월~일 단위이며, 월말에 걸친 주는 그 주 일요일이 속한 달에 계산</li>
 *   <li>주마다 PayLine 하나. 지각·조퇴는 출근으로 본다</li>
 * </ul>
 * 계약 기간(시작일~종료일) 밖의 요일은 소정근로일로 보지 않는다.
 */
public class WeeklyHolidayPayRule implements PayRule {

    private static final int MIN_WEEKLY_MINUTES = 15 * 60;
    private static final int FULL_WEEK_MINUTES = 40 * 60;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("M/d");

    @Override
    public List<PayLine> apply(PayContext ctx) {
        Contract contract = ctx.contract();
        int weeklyMinutes = WorkTime.workMinutes(
                contract.scheduledStart(), contract.scheduledEnd(), contract.scheduledBreakMinutes())
                * contract.workDays().size();
        if (weeklyMinutes < MIN_WEEKLY_MINUTES) {
            return List.of();
        }

        Set<LocalDate> attended = new HashSet<>();
        for (WorkRecord r : ctx.records()) {
            attended.add(r.date());
        }
        for (WorkRecord r : ctx.adjacentRecords()) {
            attended.add(r.date());
        }

        int wage = contract.hourlyWage();
        // 주휴 시간 = min(주 소정 분, 2,400) ÷ 40 × 8 = 주 소정 분 ÷ 5
        int paidWeekMinutes = Math.min(weeklyMinutes, FULL_WEEK_MINUTES);
        int amount = (int) (((long) paidWeekMinutes * wage * 2 + 300) / 600);
        int holidayMinutes = Math.round(paidWeekMinutes / 5f);

        List<PayLine> lines = new ArrayList<>();
        for (LocalDate sunday : sundaysOf(ctx.month())) {
            LocalDate monday = sunday.minusDays(6);
            if (!attendedAllScheduledDays(contract, monday, attended)) {
                continue;
            }
            String description = String.format("%s~%s 주휴 %s × %,d원",
                    DAY.format(monday), DAY.format(sunday), PayFormat.duration(holidayMinutes), wage);
            lines.add(new PayLine(PayItem.WEEKLY_HOLIDAY, amount, description));
        }
        return lines;
    }

    /** 일요일이 해당 월에 속하는 주들의 일요일 목록. */
    private static List<LocalDate> sundaysOf(YearMonth month) {
        List<LocalDate> sundays = new ArrayList<>();
        LocalDate sunday = month.atDay(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        while (YearMonth.from(sunday).equals(month)) {
            sundays.add(sunday);
            sunday = sunday.plusWeeks(1);
        }
        return sundays;
    }

    /** 그 주의 소정근로일이 하나 이상 있고 모두 출근했으면 true. */
    private static boolean attendedAllScheduledDays(Contract contract, LocalDate monday, Set<LocalDate> attended) {
        int scheduled = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            if (!isScheduledDay(contract, date)) {
                continue;
            }
            scheduled++;
            if (!attended.contains(date)) {
                return false;
            }
        }
        return scheduled > 0;
    }

    private static boolean isScheduledDay(Contract contract, LocalDate date) {
        if (!contract.workDays().contains(date.getDayOfWeek())) {
            return false;
        }
        if (date.isBefore(contract.startDate())) {
            return false;
        }
        return contract.endDate() == null || !date.isAfter(contract.endDate());
    }
}
