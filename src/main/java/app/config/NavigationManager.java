package app.config;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationManager {
  // 1. Biến instance duy nhất (Singleton)
  private static NavigationManager instance;
  private Stage primaryStage;

  // 2. Private constructor ngăn không cho tạo object bên ngoài
  private NavigationManager() {}

  // 3. Cung cấp global access point
  public static NavigationManager getInstance() {
    if (instance == null) {
      instance = new NavigationManager();
    }
    return instance;
  }

  public void setPrimaryStage(Stage stage) {
    this.primaryStage = stage;
  }

  // 4. Hàm chuyển màn hình (Reusable & Tối ưu)
  public void navigateTo(View view) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(view.getFxmlPath()));
      Parent root = loader.load();
      Scene scene = new Scene(root);

      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      System.err.println("Lỗi nghiêm trọng: Không thể load màn hình " + view.name());
      e.printStackTrace();
    }
  }
}
