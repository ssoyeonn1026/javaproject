package albaguard.domain;

/** 계약 시간보다 일찍 퇴근한 사유. */
public enum EarlyLeaveReason {
    /** 조기 퇴근 없음. */
    NONE("없음"),
    /** 본인 사정으로 조기 퇴근. 못 일한 시간은 임금 없음. */
    PERSONAL("본인 사정"),
    /** 고용주 지시로 조기 퇴근. 휴업수당 대상. */
    EMPLOYER_ORDER("고용주 지시");

    private final String label;

    EarlyLeaveReason(String label) {
        this.label = label;
    }

    /** 화면에 표시할 한글 이름. */
    public String label() {
        return label;
    }
}
