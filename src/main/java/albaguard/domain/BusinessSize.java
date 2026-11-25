package albaguard.domain;

/** 사업장 규모. 가산수당과 휴업수당 적용 여부를 정한다. */
public enum BusinessSize {
    /** 5인 미만 사업장. 초과근로·연장·야간·휴일 가산, 휴업수당 미적용. */
    UNDER_FIVE("5인 미만"),
    /** 5인 이상 사업장. 모든 규칙 적용. */
    FIVE_OR_MORE("5인 이상");

    private final String label;

    BusinessSize(String label) {
        this.label = label;
    }

    /** 화면에 표시할 한글 이름. */
    public String label() {
        return label;
    }
}
