package app.config;

import app.enums.View;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationManager {
  private static final NavigationManager instance = new NavigationManager();
  private Stage primaryStage;

  private NavigationManager() {}

  public static NavigationManager getInstance() {
    return instance;
  }

  public void setPrimaryStage(Stage stage) {
    this.primaryStage = stage;
  }

  // 4. Hàm chuyển màn hình (Reusable & Tối ưu)
  public void navigateTo(View view) {
    navigateTo(view, null);
  }

  public void navigateTo(View view, Consumer<Object> controllerCallback) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(view.getFxmlPath()));
      Parent root = loader.load();

      if (controllerCallback != null) {
        controllerCallback.accept(loader.getController());
      }

      Scene scene = new Scene(root);

      String css =
          Objects.requireNonNull(getClass().getResource("/app/views/style.css")).toExternalForm();
      scene.getStylesheets().add(css);

      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      System.err.println("Lỗi nghiêm trọng: Không thể load màn hình " + view.name());
      e.printStackTrace();
    }
  }
}
