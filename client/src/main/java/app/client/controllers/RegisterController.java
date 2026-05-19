package app.client.controllers;

import app.client.Client;
import app.client.controllers.manager.NavigationManager;
import app.client.utils.AlertUtils;
import app.common.dto.RegisterRequest;
import app.common.dto.RegisterResponse;
import app.common.enums.PacketType;
import app.common.enums.UserRole;
import app.common.enums.View;
import app.common.models.PacketReq;
import app.common.observer.PacketListener;
import java.io.IOException;
import java.util.Objects;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

/** RegisterController. */
public class RegisterController {
  @FXML private AnchorPane rootPane;
  @FXML private Label lblLogin;
  @FXML private TextField txtName;
  @FXML private TextField txtAccount;
  @FXML private PasswordField txtPassword;
  @FXML private RadioButton rbSeller;
  @FXML private RadioButton rbBidder;

  private final ToggleGroup roleGroup = new ToggleGroup();
  private PacketListener<RegisterResponse> registerHandler;
  @FXML private Button registerButton;

  @FXML
  private void initialize() {
    rbSeller.setToggleGroup(roleGroup);
    rbBidder.setToggleGroup(roleGroup);

    rbBidder.setSelected(true);
    try {
      String url =
          Objects.requireNonNull(getClass().getResource("/app/views/images/background_login.png"))
              .toExternalForm();
      if (rootPane != null) {
        rootPane.setStyle(
            "-fx-background-image: url('"
                + url
                + "');"
                + "-fx-background-size: cover;"
                + "-fx-background-position: center center;"
                + "-fx-background-repeat: no-repeat;"
                + "-fx-background-color: #0a0f16;");
      }
    } catch (Exception e) {
      System.err.println("Không load được background: " + e.getMessage());
    }
    registerHandler =
        (RegisterResponse response, boolean success, String message) -> {
          Platform.runLater(
              () -> {
                if (success) {
                  AlertUtils.showInfo("Thành công", message);
                  if (registerHandler != null) {
                    Client.getInstance().unsubscribe(PacketType.REGISTER, registerHandler);
                  }
                  NavigationManager.getInstance().navigateTo(View.LOGIN);
                } else {
                  AlertUtils.showError("Thất bại", message);
                }
              });
        };
    Client.getInstance().subscribe(PacketType.REGISTER, RegisterResponse.class, registerHandler);
  }

  /** Member. */
  @FXML
  public void handleRegister(ActionEvent event) {
    String name = txtName.getText();
    String account = txtAccount.getText();
    String password = txtPassword.getText();
    if (name == null
        || name.trim().isEmpty()
        || account == null
        || account.trim().isEmpty()
        || password == null
        || password.trim().isEmpty()) {
      AlertUtils.showError("Lỗi đăng ký", "Vui lòng nhập đầy đủ Name, Account và Password!");
      return;
    }
    UserRole role = rbSeller.isSelected() ? UserRole.SELLER : UserRole.BIDDER;
    RegisterRequest request = new RegisterRequest(name, account, password, role);

    try {
      Client.getInstance().sendRequest(PacketReq.of(PacketType.REGISTER, request));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  /** Member. */
  @FXML
  public void backToLoginMouse(MouseEvent event) {
    if (registerHandler != null) {
      Client.getInstance().unsubscribe(PacketType.REGISTER, registerHandler);
    }
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }
}
