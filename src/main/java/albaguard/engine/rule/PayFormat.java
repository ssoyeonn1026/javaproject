package albaguard.engine.rule;

/** 규칙들이 PayLine 설명 문장을 만들 때 함께 쓰는 표시 도우미. */
final class PayFormat {

    private PayFormat() {
    }

    /** 분을 "76시간 30분" 형태로 바꾼다. 분이 0이면 "76시간", 1시간 미만이면 "30분". */
    static String duration(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (rest == 0) {
            return hours + "시간";
        }
        return hours == 0 ? rest + "분" : hours + "시간 " + rest + "분";
    }
}
