package albaguard.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * 근로 조건. 모든 계산의 기준이며 한 개만 저장한다.
 *
 * @param workplaceName         근무지 이름. 화면과 리포트 표시용
 * @param hourlyWage            시급(원). 모든 금액 계산의 기준이자 최저임금 비교 대상
 * @param startDate             계약 시작일
 * @param endDate               계약 종료일. 기간 없는 계약은 null. 수습 감액 가능 여부(1년 이상 계약) 판단에 사용
 * @param workDays              계약 근무 요일. 주휴수당의 개근 여부와 1주 계약 근무시간 계산에 사용
 * @param scheduledStart        계약 출근 시각
 * @param scheduledEnd          계약 퇴근 시각. 조기 퇴근 시간(휴업수당) 계산 기준
 * @param scheduledBreakMinutes 계약 휴게시간(분). 1일 계약 근무시간 = 퇴근 - 출근 - 휴게
 * @param probation             수습 여부
 * @param probationEndDate      수습 종료일. 수습이 아니면 null. 수습 감액 3개월 한도 판단에 사용
 * @param businessSize          사업장 규모. 가산수당과 휴업수당 적용 여부 결정
 * @param writtenContract       근로계약서 작성 여부. false면 위반 항목으로 표시
 */
public record Contract(
        String workplaceName,
        int hourlyWage,
        LocalDate startDate,
        LocalDate endDate,
        Set<DayOfWeek> workDays,
        LocalTime scheduledStart,
        LocalTime scheduledEnd,
        int scheduledBreakMinutes,
        boolean probation,
        LocalDate probationEndDate,
        BusinessSize businessSize,
        boolean writtenContract) {

    public Contract {
        workDays = Set.copyOf(workDays);
    }
}
