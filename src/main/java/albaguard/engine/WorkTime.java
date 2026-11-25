package albaguard.engine;

import albaguard.domain.WorkRecord;

import java.time.LocalTime;

/**
 * 근무시간 계산 도우미 (docs/CALC_RULES.md 2장).
 * 모든 시간은 분 단위 int. 상태가 없는 static 메서드만 가진다.
 */
public final class WorkTime {

    private static final int DAY_MINUTES = 24 * 60;

    /** 야간 구간(출근일 0시 기준 분): 당일 0~6시, 당일 22시~다음 날 6시, 다음 날 22시~. */
    private static final int[][] NIGHT_INTERVALS = {
            {0, 6 * 60},
            {22 * 60, 30 * 60},
            {46 * 60, 54 * 60},
    };

    private WorkTime() {
    }

    /** 근무 기록 하나의 실제 근무 분 (퇴근 - 출근 - 휴게). */
    public static int workMinutes(WorkRecord record) {
        return workMinutes(record.clockIn(), record.clockOut(), record.breakMinutes());
    }

    /** 근무 기록 하나의 야간(22시~다음 날 6시) 근무 분. */
    public static int nightMinutes(WorkRecord record) {
        return nightMinutes(record.clockIn(), record.clockOut(), record.breakMinutes());
    }

    /**
     * 실제 근무 분 = (퇴근 - 출근) - 휴게. 퇴근이 출근보다 이르거나 같으면 다음 날 퇴근으로 본다.
     * 휴게가 근무 구간보다 길어도 0 미만이 되지 않는다.
     */
    public static int workMinutes(LocalTime clockIn, LocalTime clockOut, int breakMinutes) {
        return Math.max(0, span(clockIn, clockOut) - breakMinutes);
    }

    /**
     * 야간 근무 분. 근무 구간과 야간 구간이 겹치는 분을 구한다.
     * 휴게시간은 야간이 아닌 시간에 쓴 것으로 가정하므로, 야간이 아닌 시간보다 휴게가 길 때만
     * 그 초과분을 야간 분에서 뺀다.
     */
    public static int nightMinutes(LocalTime clockIn, LocalTime clockOut, int breakMinutes) {
        int start = clockIn.getHour() * 60 + clockIn.getMinute();
        int end = start + span(clockIn, clockOut);

        int night = 0;
        for (int[] interval : NIGHT_INTERVALS) {
            night += Math.max(0, Math.min(end, interval[1]) - Math.max(start, interval[0]));
        }

        int nonNight = (end - start) - night;
        int breakInNight = Math.max(0, breakMinutes - nonNight);
        return Math.max(0, night - breakInNight);
    }

    /** 휴게 전 근무 구간 길이(분). 퇴근이 출근보다 이르거나 같으면 다음 날로 계산. */
    private static int span(LocalTime clockIn, LocalTime clockOut) {
        int diff = (clockOut.getHour() * 60 + clockOut.getMinute())
                - (clockIn.getHour() * 60 + clockIn.getMinute());
        return diff <= 0 ? diff + DAY_MINUTES : diff;
    }
}
