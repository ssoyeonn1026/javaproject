package albaguard.service;

import albaguard.domain.ComparisonResult;

import java.time.YearMonth;
import java.util.Optional;

/**
 * 법정 급여와 실제 받은 금액을 비교한다.
 * 구현: DefaultComparisonService (백엔드).
 */
public interface ComparisonService {

    /**
     * calculate 결과와 세전 지급액(입금액 + 세금·보험 공제액)을 비교해
     * 전체 차액과 항목별 차액을 계산한다. 기타 공제 합계도 함께 계산한다.
     *
     * @param month 비교할 월
     * @return 비교 결과. 받은 금액이 미입력이면 빈 Optional
     * @throws ValidationException 근로 조건이 없는 경우 (급여 계산이 불가능하므로)
     */
    Optional<ComparisonResult> compare(YearMonth month);
}
