package albaguard.engine;

import albaguard.domain.BusinessSize;
import albaguard.engine.rule.BasePayRule;
import albaguard.engine.rule.HolidayPayRule;
import albaguard.engine.rule.NightPayRule;
import albaguard.engine.rule.OvertimePayRule;
import albaguard.engine.rule.PartTimeOvertimeRule;
import albaguard.engine.rule.PayRule;
import albaguard.engine.rule.ShutdownAllowanceRule;
import albaguard.engine.rule.WeeklyHolidayPayRule;

import java.util.List;

/**
 * 사업장 규모에 맞는 규칙 목록을 고른다 (docs/CALC_RULES.md 3장).
 * 새 규칙은 클래스를 만든 뒤 여기에 등록한다.
 */
public final class RuleSets {

    private RuleSets() {
    }

    /**
     * 수당 계산 규칙 목록. 5인 미만은 기본급·주휴수당 2개, 5인 이상은 초과근로·연장·휴일·야간·휴업수당을
     * 포함한 7개 전부.
     */
    public static List<PayRule> payRules(BusinessSize size) {
        if (size == BusinessSize.FIVE_OR_MORE) {
            return List.of(
                    new BasePayRule(),
                    new WeeklyHolidayPayRule(),
                    new PartTimeOvertimeRule(),
                    new OvertimePayRule(),
                    new HolidayPayRule(),
                    new NightPayRule(),
                    new ShutdownAllowanceRule());
        }
        return List.of(new BasePayRule(), new WeeklyHolidayPayRule());
    }
}
