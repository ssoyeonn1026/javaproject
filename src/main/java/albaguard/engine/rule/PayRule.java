package albaguard.engine.rule;

import albaguard.domain.PayLine;

import java.util.List;

/**
 * 수당 항목 하나를 계산하는 규칙의 공통 인터페이스.
 * 새 규칙은 이 인터페이스를 구현한 뒤 RuleSets에 등록한다.
 */
public interface PayRule {

    /**
     * 계산 재료로 수당을 계산한다.
     *
     * @param ctx 근로 조건, 해당 월, 근무 기록, 최저임금
     * @return 계산된 PayLine 목록. 해당하는 금액이 없으면 빈 목록
     */
    List<PayLine> apply(PayContext ctx);
}
