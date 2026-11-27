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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleSetsTest {

    private static List<Class<?>> classesOf(List<PayRule> rules) {
        return rules.stream().<Class<?>>map(Object::getClass).toList();
    }

    @Test
    void 오인_미만은_기본급과_주휴수당만() {
        assertEquals(List.of(BasePayRule.class, WeeklyHolidayPayRule.class),
                classesOf(RuleSets.payRules(BusinessSize.UNDER_FIVE)));
    }

    @Test
    void 오인_이상은_7개_전부() {
        assertEquals(List.of(BasePayRule.class, WeeklyHolidayPayRule.class, PartTimeOvertimeRule.class,
                        OvertimePayRule.class, HolidayPayRule.class, NightPayRule.class,
                        ShutdownAllowanceRule.class),
                classesOf(RuleSets.payRules(BusinessSize.FIVE_OR_MORE)));
    }
}
