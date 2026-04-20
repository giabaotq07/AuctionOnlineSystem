package app.controllers;

import app.config.NavigationManager;
import app.config.View;
import app.dao.UserDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

  @FXML private TextField txtName;

  @FXML private TextField txtAccount;

  @FXML private PasswordField txtPassword;

  private final UserDAO userDAO = new UserDAO();

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
      showAlert(
          Alert.AlertType.WARNING,
          "Lỗi đăng ký",
          "Vui lòng nhập đầy đủ Name, Account và Password!");
      return;
    }

    int id = userDAO.addUser(account, password, name);
    if (id > 0) {
      showAlert(
          Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công! ID tài khoản: " + id);
      // Có thể thêm code chuyển về màn hình đăng nhập ở đây
    } else {
      showAlert(
          Alert.AlertType.ERROR,
          "Thất bại",
          "Tài khoản đã tồn tại hoặc có lỗi xảy ra (kiểm tra Console).");
    }
  }

  @FXML
  public void backToLogin(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }

  private void showAlert(Alert.AlertType alertType, String title, String message) {
    Alert alert = new Alert(alertType);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
