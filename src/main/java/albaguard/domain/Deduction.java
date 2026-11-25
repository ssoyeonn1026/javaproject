package albaguard.domain;

import java.time.LocalDate;

/**
 * 급여에서 빠진 금액 한 건. 지각 벌금, 유니폼비 등.
 *
 * @param id     기록 식별자
 * @param date   공제 발생일. 해당 월 판단에 사용
 * @param amount 공제 금액(원, 양수)
 * @param reason 공제 사유. 부당 공제 위반 메시지에 사용
 */
public record Deduction(String id, LocalDate date, int amount, String reason) {
}
