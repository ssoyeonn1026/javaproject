package albaguard.service;

import albaguard.domain.Contract;
import albaguard.domain.Deduction;
import albaguard.domain.ReceivedPay;
import albaguard.domain.WorkRecord;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 사용자가 입력한 데이터(근로 조건, 근무 기록, 공제, 받은 금액)의 저장과 조회.
 * 구현: JsonRecordService (백엔드).
 */
public interface RecordService {

    /**
     * 저장된 근로 조건을 반환한다.
     *
     * @return 근로 조건. 저장된 값이 없으면 빈 Optional
     */
    Optional<Contract> getContract();

    /**
     * 근로 조건을 저장한다. 기존 값은 덮어쓴다.
     *
     * @param contract 저장할 근로 조건
     * @throws ValidationException 시급이 0 이하인 경우 등 근로 조건이 규칙에 맞지 않을 때
     */
    void saveContract(Contract contract);

    /**
     * 해당 월의 근무 기록을 날짜순으로 반환한다.
     *
     * @param month 조회할 월
     * @return 날짜순 근무 기록. 없으면 빈 목록
     */
    List<WorkRecord> getRecords(YearMonth month);

    /**
     * 근무 기록을 추가한다. 같은 날 여러 건을 추가할 수 있다.
     *
     * @param record 추가할 근무 기록
     * @throws ValidationException 근로 조건이 아직 없는 경우, 또는 기록 값이 규칙에 맞지 않을 때
     */
    void addRecord(WorkRecord record);

    /**
     * id가 같은 근무 기록을 새 값으로 교체한다.
     *
     * @param record 교체할 새 근무 기록 (id로 대상 지정)
     * @throws ValidationException 해당 id의 기록이 없는 경우, 또는 기록 값이 규칙에 맞지 않을 때
     */
    void updateRecord(WorkRecord record);

    /**
     * id로 근무 기록을 삭제한다.
     *
     * @param recordId 삭제할 근무 기록의 id
     */
    void deleteRecord(String recordId);

    /**
     * 해당 월의 공제 기록을 반환한다.
     *
     * @param month 조회할 월
     * @return 공제 기록. 없으면 빈 목록
     */
    List<Deduction> getDeductions(YearMonth month);

    /**
     * 공제 기록을 추가한다.
     *
     * @param deduction 추가할 공제 기록
     * @throws ValidationException 금액이 0 이하인 경우
     */
    void addDeduction(Deduction deduction);

    /**
     * id로 공제 기록을 삭제한다.
     *
     * @param deductionId 삭제할 공제 기록의 id
     */
    void deleteDeduction(String deductionId);

    /**
     * 해당 월에 입력한 받은 금액을 반환한다.
     *
     * @param month 조회할 월
     * @return 받은 금액. 미입력이면 빈 Optional
     */
    Optional<ReceivedPay> getReceivedPay(YearMonth month);

    /**
     * 받은 금액을 저장한다. 같은 월의 기존 값은 덮어쓴다.
     *
     * @param pay 저장할 받은 금액
     * @throws ValidationException 금액이 음수인 경우 등 값이 규칙에 맞지 않을 때
     */
    void saveReceivedPay(ReceivedPay pay);
}
