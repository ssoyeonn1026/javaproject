package albaguard.service;

import albaguard.domain.Violation;

import java.time.YearMonth;
import java.util.List;

/**
 * 법 위반 의심 항목을 검사한다.
 * 구현: DefaultViolationService (백엔드).
 */
public interface ViolationService {

    /**
     * 등록된 위반 규칙을 모두 검사해 위반 의심 항목을 반환한다.
     * 받은 금액이 필요한 규칙(주휴수당 미지급 등)은 받은 금액이 미입력이면 건너뛴다.
     *
     * @param month 검사할 월
     * @return 위반 의심 항목. 없으면 빈 목록
     * @throws ValidationException 근로 조건이 없는 경우 (급여 계산이 불가능하므로)
     */
    List<Violation> detect(YearMonth month);
}
