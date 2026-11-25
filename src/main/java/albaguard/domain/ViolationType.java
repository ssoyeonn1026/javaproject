package albaguard.domain;

/** 위반 의심 항목의 종류. */
public enum ViolationType {
    /** 최저임금 미달. */
    MINIMUM_WAGE("최저임금 미달"),
    /** 수습 감액 한도 초과 또는 감액 불가 조건에서 감액. */
    PROBATION_REDUCTION("수습 감액 위반"),
    /** 주휴수당 미지급. */
    WEEKLY_HOLIDAY_UNPAID("주휴수당 미지급"),
    /** 초과근로·연장·야간·휴일 가산수당 미지급. */
    PREMIUM_UNPAID("가산수당 미지급"),
    /** 휴게시간 부족. */
    BREAK_TIME_SHORTAGE("휴게시간 부족"),
    /** 휴업수당 미지급. */
    SHUTDOWN_ALLOWANCE_UNPAID("휴업수당 미지급"),
    /** 부당 공제. */
    UNFAIR_DEDUCTION("부당 공제"),
    /** 근로계약서 미작성. */
    NO_WRITTEN_CONTRACT("근로계약서 미작성");

    private final String label;

    ViolationType(String label) {
        this.label = label;
    }

    /** 화면에 표시할 한글 이름. */
    public String label() {
        return label;
    }
}
