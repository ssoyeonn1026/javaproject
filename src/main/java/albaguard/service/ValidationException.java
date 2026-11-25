package albaguard.service;

/**
 * 입력 값이 규칙에 맞지 않을 때 백엔드가 발생시키는 예외.
 * 메시지는 화면에 그대로 표시할 수 있는 문장으로 작성한다.
 * 예: "퇴근 시각이 출근 시각과 같습니다."
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
