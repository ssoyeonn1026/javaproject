package albaguard.engine.rule;

import albaguard.domain.Contract;
import albaguard.domain.WorkRecord;
import albaguard.engine.WorkTime;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/** 초과근로수당(R3)과 연장근로수당(R4)이 함께 쓰는 하루 단위 계산. 두 규칙이 같은 시간을 두 번 세지 않게 기준을 한 곳에 둔다. */
final class OvertimeCalc {

    /** 하루 법정 근로시간(분). */
    static final int DAY_LIMIT = 8 * 60;
    /** 1주 법정 근로시간(분). 단시간근로자 판단 기준이기도 하다. */
    static final int WEEK_LIMIT = 40 * 60;

    private OvertimeCalc() {
    }

    /** 휴일 근무 기록을 빼고, 날짜별 실근로 분 합계를 구한다. 같은 날 여러 건은 합친다. */
    static Map<LocalDate, Integer> actualByDate(Collection<WorkRecord> records) {
        Map<LocalDate, Integer> byDate = new TreeMap<>();
        for (WorkRecord r : records) {
            if (!r.holiday()) {
                byDate.merge(r.date(), WorkTime.workMinutes(r), Integer::sum);
            }
        }
        return byDate;
    }

    /** 1일 소정근로 분 = 계약 퇴근 - 계약 출근 - 계약 휴게. */
    static int scheduledDayMinutes(Contract c) {
        return WorkTime.workMinutes(c.scheduledStart(), c.scheduledEnd(), c.scheduledBreakMinutes());
    }

    /** 단시간근로자(1주 소정근로시간 40시간 미만)인지. */
    static boolean isPartTime(Contract c) {
        return scheduledDayMinutes(c) * c.workDays().size() < WEEK_LIMIT;
    }

    /**
     * R3 초과근로 분 = max(0, min(실근로 분, 480) - 그날 소정 분).
     * 계약 근무 요일이 아닌 날의 소정 분은 0. 단시간근로자가 아니면 항상 0.
     */
    static int partTimeExcess(Contract c, LocalDate date, int actualMinutes) {
        if (!isPartTime(c)) {
            return 0;
        }
        int scheduled = c.workDays().contains(date.getDayOfWeek()) ? scheduledDayMinutes(c) : 0;
        return Math.max(0, Math.min(actualMinutes, DAY_LIMIT) - scheduled);
    }

    /** R4 하루 연장 분 = max(0, 실근로 분 - 480). */
    static int dailyOvertime(int actualMinutes) {
        return Math.max(0, actualMinutes - DAY_LIMIT);
    }
}
