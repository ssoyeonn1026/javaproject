package albaguard.domain;

import java.time.YearMonth;
import java.util.List;

/**
 * 법정 급여와 받은 금액의 비교 결과. ComparisonService.compare의 반환값.
 *
 * @param month         대상 월
 * @param legalTotal    법정 급여 합계(PayrollResult.total)
 * @param receivedTotal 실제 입금액(ReceivedPay.totalAmount)
 * @param grossReceived 세전 지급액 = 입금액 + 세금·보험 공제액. 법정 급여와 비교하는 기준
 * @param deductionTotal 해당 월 공제 기록 합계
 * @param difference    legalTotal - grossReceived. 양수면 덜 받은 금액(기타 공제는 여기에 포함됨)
 * @param items         항목별 비교
 */
public record ComparisonResult(
        YearMonth month,
        int legalTotal,
        int receivedTotal,
        int grossReceived,
        int deductionTotal,
        int difference,
        List<ItemDifference> items) {

    public ComparisonResult {
        items = List.copyOf(items);
    }
}
