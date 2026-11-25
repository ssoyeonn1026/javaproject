package albaguard.domain;

/**
 * 급여 계산 결과의 한 줄. 급여 상세 표의 한 행에 해당한다.
 *
 * @param item        항목 종류(기본급, 주휴수당 등)
 * @param amount      항목 금액(원)
 * @param description 계산 근거 문장. 예: "76시간 30분 × 10,320원"
 */
public record PayLine(PayItem item, int amount, String description) {
}
