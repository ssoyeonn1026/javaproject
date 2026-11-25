package albaguard.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 실제로 일한 하루 기록 한 건.
 *
 * @param id               기록 식별자. 수정·삭제 대상 지정에 사용
 * @param date             근무 날짜. 월별 조회와 주 단위 묶음 기준
 * @param clockIn          실제 출근 시각
 * @param clockOut         실제 퇴근 시각. clockIn보다 이르면 다음 날 퇴근으로 처리
 * @param breakMinutes     실제 휴게시간(분). 근무시간에서 제외하며 휴게시간 부족 검사에 사용
 * @param holiday          휴일 근무 여부. 사용자가 직접 체크하며 휴일수당 계산에 사용
 * @param earlyLeaveReason 계약 시간보다 일찍 퇴근한 사유. EMPLOYER_ORDER면 휴업수당 대상
 * @param memo             자유 메모. 리포트에 포함
 */
public record WorkRecord(
        String id,
        LocalDate date,
        LocalTime clockIn,
        LocalTime clockOut,
        int breakMinutes,
        boolean holiday,
        EarlyLeaveReason earlyLeaveReason,
        String memo) {
}
