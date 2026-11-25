package albaguard.domain;

/** 급여 항목 종류. 계산 결과와 받은 금액 비교에서 같은 기준으로 쓴다. */
public enum PayItem {
    /** 기본급. */
    BASE("기본급"),
    /** 주휴수당. */
    WEEKLY_HOLIDAY("주휴수당"),
    /** 단시간근로자 초과근로 가산수당 (계약 시간 초과, 8시간 이내). */
    PART_TIME_OVERTIME("초과근로수당"),
    /** 연장근로 가산수당 (하루 8시간, 주 40시간 초과). */
    OVERTIME("연장근로수당"),
    /** 야간근로 가산수당. */
    NIGHT("야간근로수당"),
    /** 휴일근로 가산수당. */
    HOLIDAY("휴일근로수당"),
    /** 휴업수당. */
    SHUTDOWN_ALLOWANCE("휴업수당");

    private final String label;

    PayItem(String label) {
        this.label = label;
    }

    /** 화면에 표시할 한글 이름. */
    public String label() {
        return label;
    }
}
