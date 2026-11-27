package albaguard.engine.rule;

import albaguard.domain.BusinessSize;
import albaguard.domain.Contract;
import albaguard.domain.EarlyLeaveReason;
import albaguard.domain.PayItem;
import albaguard.domain.PayLine;
import albaguard.domain.WorkRecord;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyHolidayPayRuleTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);
    private static final int WAGE = 10_320;
    private static final Set<DayOfWeek> MON_WED_FRI_SAT =
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    /** 18:00~23:00, 휴게 30분 = 하루 270분. 4일이면 주 18시간. */
    private static Contract evening(Set<DayOfWeek> days, LocalDate start) {
        return contract(days, start, LocalTime.of(18, 0), LocalTime.of(23, 0), 30);
    }

    private static Contract contract(Set<DayOfWeek> days, LocalDate start, LocalTime in, LocalTime out, int brk) {
        return new Contract("편의점", WAGE, start, null, days, in, out, brk,
                false, null, BusinessSize.FIVE_OR_MORE, true);
    }

    private static WorkRecord work(LocalDate date) {
        return new WorkRecord("r" + date, date, LocalTime.of(18, 0), LocalTime.of(23, 0), 30,
                false, EarlyLeaveReason.NONE, "");
    }

    /** from~to 사이 날짜 중 요일에 해당하는 날의 근무 기록. */
    private static List<WorkRecord> attendance(LocalDate from, LocalDate to, Set<DayOfWeek> days) {
        List<WorkRecord> list = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (days.contains(d.getDayOfWeek())) {
                list.add(work(d));
            }
        }
        return list;
    }

    private static List<PayLine> apply(Contract c, YearMonth month, List<WorkRecord> records) {
        return new WeeklyHolidayPayRule().apply(new PayContext(c, month, records));
    }

    private static int total(List<PayLine> lines) {
        return lines.stream().mapToInt(PayLine::amount).sum();
    }

    @Test
    void 주_18시간_4주_개근이면_주당_37152원_총_148608원() {
        // 11/2 시작 계약: 11/1(일)로 끝나는 주는 계약 전이라 대상이 아니다
        Contract c = evening(MON_WED_FRI_SAT, LocalDate.of(2026, 11, 2));
        List<WorkRecord> records = attendance(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 30), MON_WED_FRI_SAT);

        List<PayLine> lines = apply(c, NOV, records);

        assertEquals(4, lines.size());
        lines.forEach(l -> {
            assertEquals(PayItem.WEEKLY_HOLIDAY, l.item());
            assertEquals(37_152, l.amount());
        });
        assertEquals(148_608, total(lines));
        assertEquals("11/2~11/8 주휴 3시간 36분 × 10,320원", lines.get(0).description());
    }

    @Test
    void 주_14시간_계약은_15시간_미만이라_0원() {
        // 하루 7시간 × 2일 = 14시간
        Set<DayOfWeek> days = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        Contract c = contract(days, LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(16, 0), 0);
        List<WorkRecord> records = attendance(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 30), days);

        assertTrue(apply(c, NOV, records).isEmpty());
    }

    @Test
    void 주_15시간_계약은_지급_대상() {
        // 하루 7시간 30분 × 2일 = 15시간 → 3시간 × 10,320 = 30,960원
        Set<DayOfWeek> days = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        Contract c = contract(days, LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(16, 30), 0);
        List<WorkRecord> records = attendance(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8), days);

        List<PayLine> lines = apply(c, NOV, records);
        assertEquals(1, lines.size());
        assertEquals(30_960, lines.get(0).amount());
    }

    @Test
    void 한_주에_하루_결근하면_그_주는_제외() {
        Contract c = evening(MON_WED_FRI_SAT, LocalDate.of(2026, 11, 2));
        List<WorkRecord> records = attendance(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 30), MON_WED_FRI_SAT);
        records.removeIf(r -> r.date().equals(LocalDate.of(2026, 11, 14))); // 둘째 주 토요일 결근

        List<PayLine> lines = apply(c, NOV, records);

        assertEquals(3, lines.size());
        assertEquals(111_456, total(lines));
        assertTrue(lines.stream().noneMatch(l -> l.description().startsWith("11/9~11/15")));
    }

    @Test
    void 지각이나_조퇴한_날도_출근으로_본다() {
        Contract c = evening(MON_WED_FRI_SAT, LocalDate.of(2026, 11, 2));
        List<WorkRecord> records = attendance(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8), MON_WED_FRI_SAT);
        records.set(0, new WorkRecord("late", LocalDate.of(2026, 11, 2), LocalTime.of(19, 0),
                LocalTime.of(21, 0), 0, false, EarlyLeaveReason.PERSONAL, ""));

        assertEquals(1, apply(c, NOV, records).size());
    }

    @Test
    void 주_40시간_계약은_8시간분_82560원() {
        Set<DayOfWeek> weekdays = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        Contract c = contract(weekdays, LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(18, 0), 60);
        List<WorkRecord> records = attendance(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8), weekdays);

        List<PayLine> lines = apply(c, NOV, records);
        assertEquals(1, lines.size());
        assertEquals(82_560, lines.get(0).amount());
        assertTrue(lines.get(0).description().contains("8시간"));
    }

    @Test
    void 주_40시간을_넘는_계약도_8시간분이_최대() {
        // 하루 10시간 × 5일 = 50시간
        Set<DayOfWeek> weekdays = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        Contract c = contract(weekdays, LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(19, 0), 0);
        List<WorkRecord> records = attendance(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8), weekdays);

        assertEquals(82_560, apply(c, NOV, records).get(0).amount());
    }

    @Test
    void 월말에_걸친_주는_다음_달_일요일_기준이라_11월_계산에서_제외() {
        Contract c = evening(MON_WED_FRI_SAT, LocalDate.of(2026, 11, 2));
        // 11/30(월)~12/6(일) 주의 11/30 출근 기록이 11월 기록에 있어도 11월 줄은 4개뿐
        List<WorkRecord> records = attendance(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 30), MON_WED_FRI_SAT);
        assertTrue(records.stream().anyMatch(r -> r.date().equals(LocalDate.of(2026, 11, 30))));

        List<PayLine> lines = apply(c, NOV, records);

        assertEquals(4, lines.size());
        assertTrue(lines.stream().noneMatch(l -> l.description().contains("11/30")));
    }

    @Test
    void 월초에_걸친_주는_그_주_일요일이_속한_달에_계산되고_이웃_달_기록으로_개근_판단() {
        Contract c = evening(MON_WED_FRI_SAT, LocalDate.of(2026, 9, 1));
        List<WorkRecord> dec = attendance(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 6), MON_WED_FRI_SAT);
        List<WorkRecord> nov = attendance(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 30), MON_WED_FRI_SAT);

        // 이웃 달(11/30) 기록이 있으면 12/6(일)이 속한 주가 12월에 계산된다
        List<PayLine> withAdjacent = new WeeklyHolidayPayRule()
                .apply(new PayContext(c, DEC, dec, nov)).stream()
                .filter(l -> l.description().startsWith("11/30~12/6")).toList();
        assertEquals(1, withAdjacent.size());
        assertEquals(37_152, withAdjacent.get(0).amount());

        // 이웃 달 기록이 없으면 11/30 출근을 확인할 수 없어 그 주는 지급하지 않는다
        assertTrue(apply(c, DEC, dec).stream().noneMatch(l -> l.description().startsWith("11/30~12/6")));
    }

    @Test
    void 계약_시작_전_주는_소정근로일이_없어_지급하지_않는다() {
        Contract c = evening(MON_WED_FRI_SAT, LocalDate.of(2026, 11, 20));
        List<WorkRecord> records = attendance(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 30), MON_WED_FRI_SAT);

        List<PayLine> lines = apply(c, NOV, records);

        // 11/20(금)이 속한 주(11/16~11/22)는 금·토만 소정근로일, 11/23~11/29 주도 개근
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).description().startsWith("11/16~11/22"));
    }

    @Test
    void 근무_기록이_없으면_주휴수당도_없다() {
        assertTrue(apply(evening(MON_WED_FRI_SAT, LocalDate.of(2026, 11, 2)), NOV, List.of()).isEmpty());
    }
}
