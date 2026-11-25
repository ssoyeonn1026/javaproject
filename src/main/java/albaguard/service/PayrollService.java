package albaguard.service;

import albaguard.domain.PayrollResult;

import java.time.YearMonth;

/**
 * 근로 조건과 근무 기록으로 법정 급여를 계산한다.
 * 구현: DefaultPayrollService (백엔드).
 */
public interface PayrollService {

    /**
     * 해당 월의 법정 급여를 항목별로 계산한다.
     * 사업장 규모에 따라 가산수당 적용 여부가 달라진다.
     *
     * @param month 계산할 월
     * @return 항목별 계산 결과. 근무 기록이 없으면 합계 0인 결과
     * @throws ValidationException 근로 조건이 없는 경우
     */
    PayrollResult calculate(YearMonth month);
}
