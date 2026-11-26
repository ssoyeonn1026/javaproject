package albaguard.engine.rule;

import albaguard.domain.Contract;
import albaguard.domain.WorkRecord;

import java.time.YearMonth;
import java.util.List;

/**
 * 수당 규칙에 넘기는 계산 재료.
 *
 * @param contract    근로 조건
 * @param month       계산 대상 월
 * @param records     해당 월의 근무 기록
 * @param minimumWage 해당 연도 최저임금(원)
 */
public record PayContext(
        Contract contract,
        YearMonth month,
        List<WorkRecord> records,
        int minimumWage) {

    public PayContext {
        records = List.copyOf(records);
    }
}
