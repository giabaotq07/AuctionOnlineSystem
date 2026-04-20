package app.controllers;

import app.config.NavigationManager;
import app.config.View;
import app.dao.UserDAO;
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
    private UserDAO userDAO;
    @FXML
    public void initialize() {
         userDAO = new UserDAO();
    }
    @FXML
    public void handleLogin(ActionEvent event) {
        String userInput = account.getText();
        String passInput = password.getText();

        // 1. Kiểm tra rỗng ở phía Client trước khi đụng vào Database
        if (userInput.isEmpty() || passInput.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ account và Password!");
            return;
        }
        // 2. Mang đi gọi hàm kiểm tra từ UserDao
        // (Thay đổi tên hàm checkCredentials cho đúng với method ông đã viết trong UserDao nhé)
        boolean isLoginSuccessful = userDAO.checkLogin(userInput, passInput);

        // 3. Xử lý kết quả trả về
        if (isLoginSuccessful) {
            // Nhảy sang màn hình chính
            try {
                SwitchToUI(event);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Không thể chuyển sang giao diện chính sau khi đăng nhập thành công.");
            }
        } else {
            // Thông báo lỗi
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Tên đăng nhập hoặc mật khẩu không chính xác!");
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
