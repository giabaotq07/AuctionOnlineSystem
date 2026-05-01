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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class RegisterController {

  @FXML private Label lblLogin;
  @FXML private TextField txtName;
  @FXML private TextField txtAccount;

  @FXML private PasswordField txtPassword;

  private final UserDAO userDAO = UserDAO.getInstance();
  private final UserService userService = new UserService(userDAO);

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
  public void backToLogin(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }

  @FXML
  public void backToLoginMouse(MouseEvent event) {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }

  @FXML
  public void handleMouseEntered(MouseEvent event) {
    if (lblLogin != null) {
      lblLogin.setUnderline(true);
    }
  }

  @FXML
  public void handleMouseExited(MouseEvent event) {
    if (lblLogin != null) {
      lblLogin.setUnderline(false);
    }
  }

  private void showAlert(Alert.AlertType alertType, String title, String message) {
    Alert alert = new Alert(alertType);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
