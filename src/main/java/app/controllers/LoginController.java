package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.dao.UserDAO;
import app.exceptions.InvalidCredentialsException;
import app.models.CommandType;
import app.models.MessagePacket;
import app.models.User;
import app.network.Client;
import app.services.UserService;
import java.io.IOException;
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
  private UserService userService;
  private UserDAO userDAO;
  private User user;

  @FXML
  public void initialize() {
    userDAO = new UserDAO();
    userService = new UserService(userDAO);
  }

  @FXML
  public void handleLogin(ActionEvent event) {
    String userInput = account.getText();
    String passInput = password.getText();

    // 1. Kiểm tra rỗng ở phía Client trước khi đụng vào Database
    if (userInput.isEmpty() || passInput.isEmpty()) {
      AlertUtils.showError("Cảnh báo", "Vui lòng nhập đầy đủ account và Password!");
      return;
    }
    try {
      user = userService.login(userInput, passInput);
      app.models.DataStore.currentUser = user; // Save logged in user
      Client.getInstance().sendRequest(new MessagePacket<>(CommandType.LOGIN, user.getName()));
      SwitchToUI(event); // Nhảy sang màn hình chính
    } catch (IOException e) {
      e.printStackTrace();
      AlertUtils.showError("Lỗi Hệ Thống", e.getMessage());
      System.out.println("Không thể chuyển sang giao diện chính sau khi đăng nhập thành công.");
    } catch (InvalidCredentialsException e) {
      AlertUtils.showError("Lỗi đăng nhập", "Tên đăng nhập hoặc mật khẩu không chính xác!");
    }
  }

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
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
