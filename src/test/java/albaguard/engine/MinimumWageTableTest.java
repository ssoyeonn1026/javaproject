package albaguard.engine;

import albaguard.service.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinimumWageTableTest {

    @Test
    void 연도별_최저임금() {
        assertEquals(10_030, MinimumWageTable.wageOf(2025));
        assertEquals(10_320, MinimumWageTable.wageOf(2026));
        assertEquals(10_700, MinimumWageTable.wageOf(2027));
    }

    @Test
    void 표에_없는_연도는_예외() {
        assertThrows(ValidationException.class, () -> MinimumWageTable.wageOf(2024));
        assertThrows(ValidationException.class, () -> MinimumWageTable.wageOf(2028));
    }
}
