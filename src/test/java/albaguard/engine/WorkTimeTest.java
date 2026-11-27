package albaguard.engine;

import albaguard.domain.EarlyLeaveReason;
import albaguard.domain.WorkRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkTimeTest {

    private static WorkRecord record(String in, String out, int breakMinutes) {
        return new WorkRecord("id", LocalDate.of(2026, 11, 2), LocalTime.parse(in), LocalTime.parse(out),
                breakMinutes, false, EarlyLeaveReason.NONE, "");
    }

    private static void check(String in, String out, int breakMinutes, int work, int night) {
        WorkRecord r = record(in, out, breakMinutes);
        assertEquals(work, WorkTime.workMinutes(r), "근무 분");
        assertEquals(night, WorkTime.nightMinutes(r), "야간 분");
    }

    @Test
    void 저녁_근무는_22시_이후만_야간() {
        check("18:00", "23:00", 30, 270, 60);
    }

    @Test
    void 자정을_넘는_근무는_다음_날_퇴근으로_계산() {
        check("22:00", "02:00", 0, 240, 240);
    }

    @Test
    void 새벽_출근은_6시까지만_야간() {
        check("05:00", "09:00", 0, 240, 60);
    }

    @Test
    void 주간_근무는_야간_없음() {
        check("09:00", "18:00", 60, 480, 0);
    }

    @Test
    void 휴게는_야간이_아닌_시간에_쓴_것으로_본다() {
        // 야간이 아닌 시간(22:00 전 4시간)이 휴게 30분보다 길어 야간 분은 그대로
        check("18:00", "02:00", 30, 450, 240);
    }

    @Test
    void 근무가_모두_야간이면_휴게만큼_야간에서_뺀다() {
        check("22:00", "02:00", 30, 210, 210);
    }

    @Test
    void 퇴근이_출근과_같으면_24시간_근무() {
        check("09:00", "09:00", 0, 1440, 480);
    }

    @Test
    void 휴게가_근무보다_길어도_음수가_되지_않는다() {
        check("09:00", "10:00", 90, 0, 0);
    }
}
