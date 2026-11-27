package albaguard.engine;

import albaguard.service.ValidationException;

import java.util.Map;

/**
 * 연도별 최저임금(시간급, 원). docs/CALC_RULES.md 6장 V1.
 * 2025년 10,030원(고용노동부 고시), 2026년 10,320원(고시 제2025-47호), 2027년 10,700원(2026-08-05 고시).
 */
public final class MinimumWageTable {

    private static final Map<Integer, Integer> WAGES = Map.of(
            2025, 10_030,
            2026, 10_320,
            2027, 10_700);

    private MinimumWageTable() {
    }

    /**
     * 해당 연도의 최저임금을 반환한다.
     *
     * @throws ValidationException 표에 없는 연도인 경우
     */
    public static int wageOf(int year) {
        Integer wage = WAGES.get(year);
        if (wage == null) {
            throw new ValidationException(year + "년 최저임금 정보가 없습니다. (2025~2027년만 지원합니다.)");
        }
        return wage;
    }
}
