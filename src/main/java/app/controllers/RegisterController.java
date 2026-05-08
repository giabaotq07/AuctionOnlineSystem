package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.dao.UserDAO;
import app.enums.UserRole;
import app.exceptions.UserAlreadyExistsException;
import app.models.*;
import app.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

public class RegisterController {

  @FXML private AnchorPane rootPane;
  @FXML private Label lblLogin;
  @FXML private TextField txtName;
  @FXML private TextField txtAccount;
  @FXML private PasswordField txtPassword;

  private final UserDAO userDAO = UserDAO.getInstance();
  private final UserService userService = new UserService(userDAO);

  @FXML
  private void initialize() {
    // Load background image giống login
    try {
      String url = getClass()
          .getResource("/app/views/images/background_login.png")
          .toExternalForm();
      if (rootPane != null) {
        rootPane.setStyle(
            "-fx-background-image: url('" + url + "');"
                + "-fx-background-size: cover;"
                + "-fx-background-position: center center;"
                + "-fx-background-repeat: no-repeat;"
                + "-fx-background-color: #0a0f16;");
      }
    } catch (Exception e) {
      System.err.println("Không load được background: " + e.getMessage());
    }
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

    User newUser =
        UserFactory.createUser(name, new Account(account, password), new Wallet(), UserRole.BIDDER);
    try {
      if (userService.register(newUser)) {
        newUser = userService.getUserByAccount(newUser.getAccount().getUsername());
      }
      AlertUtils.showInfo("Thành công", "Đăng ký thành công! ID tài khoản: " + newUser.getId());
    } catch (UserAlreadyExistsException e) {
      AlertUtils.showError(
          "Thất bại", "Tài khoản đã tồn tại hoặc có lỗi xảy ra (kiểm tra Console).");
    }
  }

  @FXML
  public void backToLoginMouse(MouseEvent event) {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }
}
