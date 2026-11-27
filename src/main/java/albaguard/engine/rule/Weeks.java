package albaguard.engine.rule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** 주(월~일) 계산 도우미. 월말에 걸친 주는 그 주 일요일이 속한 달에 계산한다는 규칙을 한 곳에 둔다. */
final class Weeks {

    private Weeks() {
    }

    /** 일요일이 해당 월에 속하는 주들의 일요일 목록. */
    static List<LocalDate> sundaysOf(YearMonth month) {
        List<LocalDate> sundays = new ArrayList<>();
        LocalDate sunday = month.atDay(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        while (YearMonth.from(sunday).equals(month)) {
            sundays.add(sunday);
            sunday = sunday.plusWeeks(1);
        }
        return sundays;
    }
}
