package albaguard;

import javafx.application.Application;

/**
 * 실행 시작 클래스.
 * Application을 상속한 App을 직접 main으로 실행하면 모듈 설정 없이 실행할 때
 * "JavaFX 런타임 구성 요소가 없습니다" 오류가 나므로, 상속하지 않은 클래스를 시작점으로 둔다.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
