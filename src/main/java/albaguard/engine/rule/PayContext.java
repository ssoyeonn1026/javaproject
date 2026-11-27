package albaguard.engine.rule;

import albaguard.domain.Contract;
import albaguard.domain.WorkRecord;

import java.time.YearMonth;
import java.util.List;

/**
 * 수당 규칙에 넘기는 계산 재료.
 *
 * @param contract        근로 조건
 * @param month           계산 대상 월
 * @param records         해당 월의 근무 기록. 기본급, 야간수당 등 월 단위 계산의 대상
 * @param adjacentRecords 이웃 달의 근무 기록. 월 경계에 걸친 주의 개근 여부(주휴수당) 판단에만 쓰고,
 *                        금액 계산 대상에는 포함하지 않는다. 없으면 빈 목록
 */
public record PayContext(
        Contract contract,
        YearMonth month,
        List<WorkRecord> records,
        List<WorkRecord> adjacentRecords) {

    public PayContext {
        records = List.copyOf(records);
        adjacentRecords = List.copyOf(adjacentRecords);
    }

    /** 이웃 달 기록 없이 만든다. */
    public PayContext(Contract contract, YearMonth month, List<WorkRecord> records) {
        this(contract, month, records, List.of());
    }
}
