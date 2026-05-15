package app.controllers;

import app.controllers.manager.NavigationManager;
import app.dto.LoginRequest;
import app.dto.LoginResponse;
import app.enums.PacketType;
import app.enums.View;
import app.models.DataStore;
import app.models.PacketReq;
import app.models.User;
import app.models.UserFactory;
import app.network.Client;
import app.network.PacketListener;
import app.utils.AlertUtils;
import java.io.IOException;
import java.util.Objects;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

/** LoginController. */
public class LoginController {
  @FXML private TextField account;
  @FXML private PasswordField password;
  @FXML private Button loginButton;
  @FXML private Label lblRegister;
  @FXML private AnchorPane rootPane;
  private PacketListener<LoginResponse> loginHandler;

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
    loginHandler =
        (LoginResponse response, boolean success, String message) ->
            Platform.runLater(
                () -> {
                  loginButton.setDisable(false);
                  if (success && response != null && response.user() != null) {
                    User user = UserFactory.createUser(response.user());
                    Client.getInstance().setCurrentUser(user);
                    DataStore.getInstance().updateCurrentUser(response.user());
                    switchToUi();
                  } else {
                    AlertUtils.showError("Đăng nhập thất bại", message);
                  }
                });
    Client.getInstance().subscribe(PacketType.LOGIN, loginHandler);
  }

  /** Member. */
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
      Client.getInstance().sendRequest(PacketReq.of(PacketType.LOGIN, loginRequest));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
      loginButton.setDisable(false);
    }
  }

  /** Member. */
  @FXML
  public void switchToUi() {
    if (loginHandler != null) {
      Client.getInstance().unsubscribe(PacketType.LOGIN, loginHandler);
    }
    DataStore.getInstance();
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  private void showAlert() {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Cảnh báo");
    alert.setHeaderText(null);
    alert.setContentText("Vui lòng nhập đầy đủ account và Password!");
    alert.showAndWait();
  }

  /** Member. */
  @FXML
  public void handleMouseEntered(MouseEvent event) {
    lblRegister.setUnderline(true);
  }

  /** Member. */
  @FXML
  public void handleMouseExited(MouseEvent event) {
    lblRegister.setUnderline(false);
  }

  /** Member. */
  @FXML
  public void switchToRegister(MouseEvent event) {
    NavigationManager.getInstance().navigateTo(View.REGISTER);
  }
}
