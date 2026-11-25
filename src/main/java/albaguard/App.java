package albaguard;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** JavaFX 애플리케이션. 지금은 PrimerLight 테마의 빈 창만 띄운다. */
public class App extends Application {
    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        stage.setTitle("알바 지킴이");
        stage.setScene(new Scene(new StackPane(), 960, 640));
        stage.show();
    }
}
