package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.utils.AlertUtils;
import app.common.enums.View;
import app.common.models.User;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UserProfileController. */
public class UserProfileController {
  private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

  @FXML private AnchorPane rootPane;
  @FXML private Circle avatarCircle;
  @FXML private Label avatarLabel;
  @FXML private ImageView avatarImageView;
  @FXML private Label userNameLabel;
  @FXML private Label emailLabel;
  @FXML private Label walletLabel;
  @FXML private Label roleLabel;
  @FXML private Label idLabel;
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

  private final Runnable updateListener = () -> Platform.runLater(this::loadUserProfile);
  private final java.util.function.Consumer<String> messageListener =
      msg ->
          Platform.runLater(
              () -> {
                if (UserManager.getInstance().getCurrentUser() == null) {
                  return;
                }
                if (msg != null
                    && (msg.contains("thành công")
                        || msg.toLowerCase().contains("success")
                        || msg.equals("OK"))) {
                  AlertUtils.showInfo("Thành công", msg);
                } else if (msg != null && !msg.isBlank()) {
                  AlertUtils.showError("Lỗi", msg);
                }
              });

  /** Member. */
  @FXML
  public void initialize() {
    ClientNotificationCenter.getInstance().addUpdateListener(updateListener);
    ClientNotificationCenter.getInstance().addMessageListener(messageListener);
    loadUserProfile();
  }

  private void loadUserProfile() {
    User currentUser = UserManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      logger.warn("loadUserProfile: current user is null, skipping profile load.");
      return;
    }
    if (avatarCircle != null) {
      avatarCircle.setFill(Color.web("#5146f2"));
      avatarCircle.setRadius(70);
    }
    if (avatarLabel != null) {
      avatarLabel.setVisible(true);
      avatarLabel.setManaged(true);
      String firstLetter = currentUser.getName().substring(0, 1).toUpperCase();
      avatarLabel.setText(firstLetter);
    }
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
      BigDecimal balance = currentUser.getWallet().getTotalBalance();
      walletLabel.setText(String.format("💰 $%s", currencyFormat.format(balance)));
      if (balance.compareTo(new BigDecimal("1000000")) < 0) {
        walletLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
      } else {
        walletLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
      }
    }
    // Tải avatar từ server/cache
    loadAvatarFromServer(currentUser);
  }

  // === AVATAR FEATURE ===

  private void loadAvatarFromServer(User currentUser) {
    String avatarUrl = currentUser.getAvatarUrl();
    if (avatarUrl == null || avatarUrl.isBlank()) {
      return;
    }
    java.util.Optional<String> base64Opt =
        UserManager.getInstance().getAvatarBase64(currentUser.getId());
    if (base64Opt.isPresent()) {
      try {
        byte[] bytes = java.util.Base64.getDecoder().decode(base64Opt.get());
        Image image = new Image(new java.io.ByteArrayInputStream(bytes));
        applyAvatarImage(image);
      } catch (Exception e) {
        logger.error("Failed to load avatar from cache", e);
      }
    } else {
      try {
        ClientRequestService.getInstance().fetchAvatar(currentUser.getId(), avatarUrl);
      } catch (IOException e) {
        logger.error("Failed to request avatar from server", e);
      }
    }
  }

  /** Xử lý khi nhấn nút "Đổi Avatar". */
  @FXML
  public void handleChangeAvatar(ActionEvent event) {
    User currentUser = UserManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      AlertUtils.showError("Lỗi", "Bạn chưa đăng nhập!");
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn ảnh avatar");
    fileChooser
        .getExtensionFilters()
        .addAll(
            new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));

    File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
    if (selectedFile == null) {
      return; // User cancelled
    }

    try {
      ClientRequestService.getInstance().uploadAvatar(selectedFile);
    } catch (Exception e) {
      logger.error("Failed to change avatar", e);
      AlertUtils.showError("Lỗi", "Không thể thay đổi avatar: " + e.getMessage());
    }
  }

  /** Áp dụng ảnh avatar lên Circle (circular clip). */
  private void applyAvatarImage(Image image) {
    if (avatarCircle == null) {
      return;
    }
    // Dùng ImagePattern để fill Circle với ảnh → tự tạo hiệu ứng tròn
    avatarCircle.setFill(new ImagePattern(image));

    // Ẩn label chữ cái khi đã có ảnh
    if (avatarLabel != null) {
      avatarLabel.setVisible(false);
      avatarLabel.setManaged(false);
    }
  }

  /** Member. */
  @FXML
  public void handleLogout(ActionEvent event) {
    try {
      ClientNotificationCenter.getInstance().removeUpdateListener(updateListener);
      ClientNotificationCenter.getInstance().removeMessageListener(messageListener);
      UserManager.getInstance().setCurrentUser(null);
      AuctionStore.getInstance().clearHistory();
      AlertUtils.showInfo("Thành công", "Đã đăng xuất!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
    } catch (Exception e) {
      AlertUtils.showError("Lỗi", "Không thể đăng xuất: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Member. */
  @FXML
  public void handleBack(ActionEvent event) {
    try {
      ClientNotificationCenter.getInstance().removeUpdateListener(updateListener);
      ClientNotificationCenter.getInstance().removeMessageListener(messageListener);
      NavigationManager.getInstance().navigateTo(View.UI);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
