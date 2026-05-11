package app.controllers;

import app.config.NavigationManager;
import app.data.LoginRequest;
import app.data.LoginResponse;
import app.enums.PacketType;
import app.enums.View;
import app.models.Packet;
import app.models.User;
import app.models.UserFactory;
import app.network.Client;
import app.utils.AlertUtils;
import app.utils.JsonUtil;
import java.io.IOException;
import java.util.Objects;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

public class LoginController {
  @FXML private TextField account;
  @FXML private PasswordField password;
  @FXML private Button loginButton;
  @FXML private Label lblRegister;
  @FXML private AnchorPane rootPane;
  private LoginResponse response;

  @FXML
  private void initialize() {
    String url =
        Objects.requireNonNull(getClass().getResource("/app/views/images/background_login.png"))
            .toExternalForm();

    rootPane.setStyle(
        "-fx-background-image: url('"
            + url
            + "');"
            + "-fx-background-size: cover;"
            + "-fx-background-position: center center;"
            + "-fx-background-repeat: no-repeat;");
    Client.getInstance()
        .setOnMessageReceived(
            packet ->
                Platform.runLater(
                    () -> {
                      loginButton.setDisable(false);
                      if (packet.getType() == PacketType.LOGIN) {
                        response = JsonUtil.fromJson(packet.getData(), LoginResponse.class);
                      }
                      if (response.success()) {
                        User user = UserFactory.createUser(response.user());
                        Client.getInstance().setCurrentUser(user);
                        SwitchToUI();
                      } else {
                        AlertUtils.showError("Đăng nhập thất bại", response.message());
                      }
                    }));
  }

  @FXML
  public void handleLogin() {
    loginButton.setDisable(true);
    String userInput = account.getText();
    String passInput = password.getText();
    if (userInput.isEmpty() || passInput.isEmpty()) {
      showAlert();
      loginButton.setDisable(false);
      return;
    }
    LoginRequest loginRequest = new LoginRequest(userInput, passInput);
    try {
      Client.getInstance().sendRequest(new Packet(PacketType.LOGIN, JsonUtil.toJson(loginRequest)));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
      loginButton.setDisable(false);
    }
  }

  @FXML
  public void SwitchToUI() {
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  private void showAlert() {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Cảnh báo");
    alert.setHeaderText(null);
    alert.setContentText("Vui lòng nhập đầy đủ account và Password!");
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
