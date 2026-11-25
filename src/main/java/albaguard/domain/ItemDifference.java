package albaguard.domain;

/**
 * 항목별 비교 결과 한 줄. 비교 표의 한 행에 해당한다.
 *
 * @param item           비교 항목
 * @param legalAmount    법정 금액(원)
 * @param receivedAmount 명세서에 적힌 받은 금액(원). 항목별 입력이 없으면 null
 * @param difference     legalAmount - receivedAmount. receivedAmount가 null이면 0
 */
public record ItemDifference(
        PayItem item,
        int legalAmount,
        Integer receivedAmount,
        int difference) {
}
