package albaguard.domain;

import java.time.YearMonth;
import java.util.List;

/**
 * 법정 급여 계산 결과. PayrollService.calculate의 반환값.
 *
 * @param month            대상 월
 * @param totalWorkMinutes 해당 월 총 근무시간(분). 대시보드 카드 표시용
 * @param lines            항목별 계산 내역
 * @param total            법정 급여 합계(원)
 */
public record PayrollResult(
        YearMonth month,
        int totalWorkMinutes,
        List<PayLine> lines,
        int total) {

    public PayrollResult {
        lines = List.copyOf(lines);
    }
}
