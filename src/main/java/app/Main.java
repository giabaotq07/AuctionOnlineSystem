package app;

import atlantafx.base.theme.PrimerDark;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
  @Override
  public void start(Stage stage) throws IOException {
    Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
    FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/app/views/ConnectServerController.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
    // String css = this.getClass().getResource("style.css").toExternalForm();
    // scene.getStylesheets().add(css);
    Image icon = new Image(getClass().getResourceAsStream("/app/views/icon.png"));
    stage.getIcons().add(icon);
    stage.setTitle("LoPPy");
    stage.setScene(scene);
    stage.show();
  }
}
