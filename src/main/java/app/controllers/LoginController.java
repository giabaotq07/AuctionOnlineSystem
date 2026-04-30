package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.dto.LoginRequest;
import app.dto.LoginResponse;
import app.enums.CommandType;
import app.models.MessagePacket;
import app.models.User;
import app.network.Client;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class LoginController {
  @FXML private TextField account;
  @FXML private PasswordField password;
  @FXML private Button loginButton;
  @FXML private Stage stage;
  @FXML private Scene scene;
  @FXML private Label lblRegister;
  private LoginRequest loginRequest;
  private LoginResponse response;
  private User user;

  @FXML
  private void initialize() {
    Client.getInstance()
        .setOnMessageReceived(
            packet -> {
              Platform.runLater(
                  () -> {
                    loginButton.setDisable(false);
                    if (packet.getType() == CommandType.LOGIN) {
                      response = (LoginResponse) packet.getData();
                    }
                    if (response.success()) {
                      SwitchToUI();
                    } else {
                      AlertUtils.showError("Đăng nhập thất bại", response.message());
                    }
                  });
            });
  }

  @FXML
  public void handleLogin(ActionEvent actionEvent) {
    loginButton.setDisable(true);
    String userInput = account.getText();
    String passInput = password.getText();
    if (userInput.isEmpty() || passInput.isEmpty()) {
      showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ account và Password!");
      loginButton.setDisable(false);
      return;
    }
    loginRequest = new LoginRequest(userInput, passInput);
    Client.getInstance().sendRequest(new MessagePacket<>(CommandType.LOGIN, loginRequest));
  }

  @FXML
  public void SwitchToUI() {
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  private void showAlert(Alert.AlertType alertType, String title, String content) {
    Alert alert = new Alert(alertType);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  // 1. Hiện dấu gạch chân khi đưa chuột vào
  @FXML
  public void handleMouseEntered(MouseEvent event) {
    lblRegister.setUnderline(true);
  }

  // 2. Bỏ dấu gạch chân khi đưa chuột ra
  @FXML
  public void handleMouseExited(MouseEvent event) {
    lblRegister.setUnderline(false);
  }

  // 3. Chuyển sang màn hình Register khi Click
  @FXML
  public void switchToRegister(MouseEvent event) {
    NavigationManager.getInstance().navigateTo(View.REGISTER);
  }
}
