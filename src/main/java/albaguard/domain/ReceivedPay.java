package albaguard.domain;

import java.time.YearMonth;
import java.util.Map;

/**
 * 한 달 동안 실제로 받은 급여.
 *
 * @param month           대상 월
 * @param totalAmount     실제 통장 입금액(원)
 * @param taxAndInsurance 세금·4대보험 공제액(3.3% 원천징수 포함). 합법적으로 뗀 금액이라 덜 받은 돈에서 제외. 없으면 0
 * @param breakdown       급여명세서의 항목별 금액. 명세서가 없으면 빈 Map. 있으면 항목별 비교에 사용
 */
public record ReceivedPay(
        YearMonth month,
        int totalAmount,
        int taxAndInsurance,
        Map<PayItem, Integer> breakdown) {

    public ReceivedPay {
        breakdown = Map.copyOf(breakdown);
    }
}
