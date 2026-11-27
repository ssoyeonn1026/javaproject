package albaguard.engine.rule;

import albaguard.domain.BusinessSize;
import albaguard.domain.Contract;
import albaguard.domain.EarlyLeaveReason;
import albaguard.domain.WorkRecord;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** 초과·연장 규칙 테스트가 같이 쓰는 데이터 생성 도우미. */
final class OvertimeTestSupport {

    static final int WAGE = 10_320;

    private OvertimeTestSupport() {
    }

    /** 월요일 하루만 일하는 18:00~23:00(휴게 30분, 하루 270분) 계약 → 주 4.5시간, 단시간근로자. */
    static Contract mondayEvening() {
        return contract(Set.of(DayOfWeek.MONDAY), LocalTime.of(18, 0), LocalTime.of(23, 0), 30);
    }

    /** 월~금 09:00~18:00(휴게 60분, 하루 480분) 계약 → 주 40시간. */
    static Contract fullTime() {
        return contract(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), LocalTime.of(9, 0), LocalTime.of(18, 0), 60);
    }

    static Contract contract(Set<DayOfWeek> days, LocalTime in, LocalTime out, int breakMinutes) {
        return new Contract("편의점", WAGE, LocalDate.of(2026, 9, 1), null, days, in, out, breakMinutes,
                false, null, BusinessSize.FIVE_OR_MORE, true);
    }

    static WorkRecord record(String id, LocalDate date, String in, String out, int breakMinutes, boolean holiday) {
        return new WorkRecord(id, date, LocalTime.parse(in), LocalTime.parse(out), breakMinutes, holiday,
                EarlyLeaveReason.NONE, "");
    }

    static WorkRecord record(LocalDate date, String in, String out, int breakMinutes) {
        return record("r-" + date + "-" + in, date, in, out, breakMinutes, false);
    }
}
