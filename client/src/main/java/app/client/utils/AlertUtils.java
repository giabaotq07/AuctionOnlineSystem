package app.client.utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import javafx.scene.control.Alert;

/** AlertUtils. */
public class AlertUtils {
  private static final Set<String> visibleAlerts = ConcurrentHashMap.newKeySet();

  /** showError. */
  public static void showError(String header, String content) {
    show(Alert.AlertType.ERROR, "Lỗi", header, content);
  }

  /** showInfo. */
  public static void showInfo(String header, String content) {
    show(Alert.AlertType.INFORMATION, "Thông báo", header, content);
  }

  private static void show(Alert.AlertType type, String title, String header, String content) {
    Runnable task = () -> showOnFxThread(type, title, header, content);
    if (Platform.isFxApplicationThread()) {
      task.run();
      return;
    }
    Platform.runLater(task);
  }

  private static void showOnFxThread(
      Alert.AlertType type, String title, String header, String content) {
    String key = type + "|" + header + "|" + content;
    if (!visibleAlerts.add(key)) {
      return;
    }
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.setOnHidden(event -> visibleAlerts.remove(key));
    alert.show();
  }
}
