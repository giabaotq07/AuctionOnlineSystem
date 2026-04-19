package app.controllers;

import app.dao.UserDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Button loginButton;
    @FXML
    private Stage stage;
    @FXML
    private Scene scene;
    @FXML
    public void handleLogin(ActionEvent event)  {
        String userInput = username.getText();
        String passInput = password.getText();

        // 1. Kiểm tra rỗng ở phía Client trước khi đụng vào Database
        if (userInput.isEmpty() || passInput.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ Username và Password!");
            return;
        }
        // 2. Mang đi gọi hàm kiểm tra từ UserDao
        // (Thay đổi tên hàm checkCredentials cho đúng với method ông đã viết trong UserDao nhé)
        UserDAO userDao = new UserDAO();
        boolean isLoginSuccessful = userDao.checkLogin(userInput, passInput);

        // 3. Xử lý kết quả trả về
        if (isLoginSuccessful) {
            // Nhảy sang màn hình chính
            try{
                SwitchToUI(event);
            }
            catch (IOException e){
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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/user_interface.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(loader.load(), 1280, 720);
        stage.setScene(scene);
        // String css = this.getClass().getResource("style.css").toExternalForm();
        // scene.getStylesheets().add(css);
        stage.show();
    }
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
