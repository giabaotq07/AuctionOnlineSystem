package app.controllers;

import app.controllers.manager.NavigationManager;
import app.enums.View;
import app.models.User;
import app.network.Client;
import app.utils.AlertUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class UserProfileController {

  @FXML private AnchorPane rootPane;
  @FXML private Circle avatarCircle;
  @FXML private Label avatarLabel;
  @FXML private Label userNameLabel;
  @FXML private Label emailLabel;
  @FXML private Label walletLabel;
  @FXML private Label roleLabel;
  @FXML private Label idLabel;

  @FXML
  public void initialize() {
    loadUserProfile();
  }

  private void loadUserProfile() {
    User currentUser = Client.getInstance().getCurrentUser();

    if (currentUser == null) {
      AlertUtils.showError("Lỗi", "Không tìm thấy thông tin user!");
      try {
        NavigationManager.getInstance().navigateTo(View.LOGIN);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }

    // ===== HIỂN THỊ AVATAR =====
    if (avatarCircle != null) {
      avatarCircle.setFill(Color.web("#673ab7")); // Màu tím chính
      avatarCircle.setRadius(80);
    }

    if (avatarLabel != null) {
      String firstLetter = currentUser.getName().substring(0, 1).toUpperCase();
      avatarLabel.setText(firstLetter);
    }

    // ===== HIỂN THỊ THÔNG TIN =====
    if (userNameLabel != null) {
      userNameLabel.setText(currentUser.getName());
    }

    if (emailLabel != null) {
      emailLabel.setText("📧 " + currentUser.getAccount().getUsername());
    }

    if (idLabel != null) {
      idLabel.setText("ID: #" + currentUser.getId());
    }

    if (roleLabel != null) {
      roleLabel.setText("🎯 " + currentUser.getRole());
    }

    if (walletLabel != null) {
      long balance = 1000;
      walletLabel.setText(String.format("💰 %,d đ", balance));

      // Đổi màu dựa trên số dư
      if (balance < 1000000) {
        walletLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;"); // Đỏ
      } else {
        walletLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;"); // Xanh
      }
    }
  }

  @FXML
  public void handleLogout(ActionEvent event) {
    try {
      // Xóa user từ Client
      Client.getInstance().setCurrentUser(null);

      AlertUtils.showInfo("Thành công", "Đã đăng xuất!");

      // Quay lại login
      NavigationManager.getInstance().navigateTo(View.LOGIN);
    } catch (Exception e) {
      AlertUtils.showError("Lỗi", "Không thể đăng xuất: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    try {
      NavigationManager.getInstance().navigateTo(View.UI);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
