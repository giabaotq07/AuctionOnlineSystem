package app.controllers;

import app.network.Client;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConnectServerController {
  @FXML private Button connectButton;
  @FXML private Label statusLabel;
  private Client client;
  private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds

  @FXML
  public void ConnectServer(ActionEvent event) {
    // Disable button and show loading state
    if (connectButton != null) {
      connectButton.setDisable(true);
      connectButton.setText("Đang kết nối...");
    }

    if (statusLabel != null) {
      statusLabel.setText("Đang kết nối tới Server...");
      statusLabel.setStyle("-fx-text-fill: #FFA500;"); // Orange for connecting
    }

    // Connect asynchronously to prevent UI freezing
    Thread connectionThread = new Thread(() -> {
      try {
        // Try to establish connection with timeout
        Client connectedClient = connectWithTimeout();

        if (connectedClient != null) {
          this.client = connectedClient;

          // Connection successful
          Platform.runLater(() -> {
            if (statusLabel != null) {
              statusLabel.setText("✓ Kết nối thành công!");
              statusLabel.setStyle("-fx-text-fill: #00AA00;"); // Green for success
            }
            try {
              SwitchToUI(event);
            } catch (IOException e) {
              showAlert("Lỗi", "Không thể tải giao diện");
              resetConnectionButton();
            }
          });
        } else {
          throw new IOException("Timeout: Không thể kết nối trong " + CONNECTION_TIMEOUT + "ms");
        }
      } catch (IOException e) {
        Platform.runLater(() -> {
          String errorMsg = getDetailedErrorMessage(e);
          showAlert("Lỗi kết nối", errorMsg);
          if (statusLabel != null) {
            statusLabel.setText("✗ Kết nối thất bại");
            statusLabel.setStyle("-fx-text-fill: #FF0000;"); // Red for error
          }
          resetConnectionButton();
        });
      } catch (Exception e) {
        Platform.runLater(() -> {
          showAlert("Lỗi không xác định", e.getMessage());
          resetConnectionButton();
        });
      }
    });

    connectionThread.setDaemon(true);
    connectionThread.start();
  }

  private Client connectWithTimeout() throws IOException, InterruptedException {
    final Client[] result = {null};
    final Exception[] exception = {null};

    Thread connectionAttempt = new Thread(() -> {
      try {
        result[0] = Client.getInstance();
      } catch (Exception e) {
        exception[0] = e;
      }
    });

    connectionAttempt.setDaemon(true);
    connectionAttempt.start();

    // Wait for connection with timeout
    connectionAttempt.join(CONNECTION_TIMEOUT);

    if (exception[0] != null) {
      throw new IOException(exception[0]);
    }

    return result[0];
  }

  private String getDetailedErrorMessage(Exception e) {
    String message = e.getMessage();
    if (message == null || message.isEmpty()) {
      message = e.getClass().getSimpleName();
    }

    if (message.contains("Connection refused")) {
      return "Server không phản hồi. Vui lòng kiểm tra:\n- Server đã khởi động chưa?\n- Địa chỉ IP và cổng có đúng không?";
    } else if (message.contains("Timeout")) {
      return "Hết thời gian chờ kết nối. Server không phản hồi.";
    } else if (message.contains("UnknownHostException")) {
      return "Không tìm thấy Server. Kiểm tra địa chỉ IP.";
    } else if (message.contains("Network is unreachable")) {
      return "Không thể truy cập mạng. Kiểm tra kết nối Internet.";
    }

    return "Lỗi kết nối: " + message;
  }

  private void resetConnectionButton() {
    if (connectButton != null) {
      connectButton.setDisable(false);
      connectButton.setText("Kết nối");
    }
  }


  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/login_scene.fxml"));
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Scene scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }

  private void showAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
