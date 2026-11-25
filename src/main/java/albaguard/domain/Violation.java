package albaguard.domain;

import java.time.LocalDate;

/**
 * 위반 의심 항목 한 건. 위반 알림 화면의 카드 한 장에 해당한다.
 *
 * @param type       위반 종류. 화면의 분류 표시에 사용
 * @param date       관련 날짜. 월 전체에 해당하면 null
 * @param amount     관련 금액(미지급액, 공제액 등, 원). 금액과 무관하면 0
 * @param message    화면에 표시할 설명 문장
 * @param legalBasis 근거 법 조항. 예: "근로기준법 제55조"
 */
public record Violation(
        ViolationType type,
        LocalDate date,
        int amount,
        String message,
        String legalBasis) {
}
