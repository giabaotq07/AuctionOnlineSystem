package app.controllers;

import app.controllers.manager.NavigationManager;
import app.data.RegisterRequest;
import app.data.RegisterResponse;
import app.enums.PacketType;
import app.enums.UserRole;
import app.enums.View;
import app.models.PacketReq;
import app.network.Client;
import app.network.PacketListener;
import app.utils.AlertUtils;
import java.io.IOException;
import java.util.Objects;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

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

  @FXML
  private void initialize() {
    //role
    rbSeller.setToggleGroup(roleGroup);
    rbBidder.setToggleGroup(roleGroup);

    rbBidder.setSelected(true);
    // Load background image giống login
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
        (RegisterResponse response) -> {
          Platform.runLater(
              () -> {
                if (response.success()) {
                  AlertUtils.showInfo("Thành công", response.message());
                  if (registerHandler != null) {
                    Client.getInstance().unsubscribe(PacketType.REGISTER, registerHandler);
                  }
                  NavigationManager.getInstance().navigateTo(View.LOGIN);
                } else {
                  AlertUtils.showError("Thất bại", response.message());
                }
              });
        };
    Client.getInstance().subscribe(PacketType.REGISTER, registerHandler);
  }

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
    UserRole role =
            rbSeller.isSelected()
                    ? UserRole.SELLER
                    : UserRole.BIDDER;
    RegisterRequest request = new RegisterRequest(name, account, password, role);

    try {
      Client.getInstance().sendRequest(PacketReq.of(PacketType.REGISTER, request));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  @FXML
  public void backToLoginMouse(MouseEvent event) {
    if (registerHandler != null) {
      Client.getInstance().unsubscribe(PacketType.REGISTER, registerHandler);
    }
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }
}
