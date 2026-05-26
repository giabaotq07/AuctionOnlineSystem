package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.ItemStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.CreateAuctionRequest;
import app.common.enums.ItemType;
import app.common.enums.View;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

/** AuctionController. */
public class AuctionController implements Cleanable {
  @FXML private TextField nameField;
  @FXML private TextArea descriptionField;
  @FXML private TextField startingPriceField;
  @FXML private TextField stepPriceField;
  @FXML private ComboBox<ItemType> typeComboBox;
  @FXML private DatePicker endDatePicker;
  @FXML private TextField endTimeField;
  @FXML private DatePicker startDatePicker;
  @FXML private TextField startTimeField;
  @FXML private Button chooseImageButton;
  @FXML private Label imageFileNameLabel;
  private File selectedImageFile;
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private boolean createLoading;
  private Button createButton;
  private Runnable stopCreateLoading = () -> {};
  private final Consumer<String> createAuctionListener =
      message -> Platform.runLater(() -> handleCreateAuctionResult(message));

  /** Member. */
  @FXML
  public void initialize() {
    notifications.addMessageListener(createAuctionListener);
    if (UserManager.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tổ chức phiên đấu giá!");
      Platform.runLater(() -> NavigationManager.getInstance().navigateTo(View.LOGIN));
      return;
    }
    if (typeComboBox != null) {
      typeComboBox.getItems().setAll(ItemType.values());
      typeComboBox.getSelectionModel().selectFirst();
    }
  }

  /** Member. */
  @FXML
  public void handleAdd(ActionEvent event) {
    if (createLoading) {
      return;
    }
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server.");
      return;
    }
    if (UserManager.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }

    try {
      String name = nameField.getText();
      String desc = descriptionField.getText();
      String startingPriceStr = startingPriceField.getText();
      String stepPriceStr = stepPriceField.getText();

      LocalDate startDate = startDatePicker.getValue();
      String timeStr = startTimeField.getText();
      LocalDate endDate = endDatePicker.getValue();
      String endTimeStr = endTimeField.getText();

      if (name == null
          || name.isEmpty()
          || desc == null
          || desc.isEmpty()
          || startingPriceStr == null
          || startingPriceStr.isEmpty()
          || stepPriceStr == null
          || stepPriceStr.isEmpty()
          || startDate == null
          || timeStr == null
          || timeStr.isEmpty()
          || endDate == null
          || endTimeStr == null
          || endTimeStr.isEmpty()) {
        AlertUtils.showError("Lỗi", "Thiếu thông tin.");
        return;
      }

      long startPrice = Long.parseLong(startingPriceStr);
      long stepPrice = Long.parseLong(stepPriceStr);
      ItemType type = typeComboBox.getValue();

      LocalTime startTimePicker;
      try {
        startTimePicker = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
      } catch (DateTimeParseException e) {
        AlertUtils.showError("Sai định dạng", "Giờ bắt đầu phải có định dạng HH:mm");
        return;
      }
      LocalDateTime startTime = LocalDateTime.of(startDate, startTimePicker);

      LocalTime endTimePicker;
      try {
        endTimePicker = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
      } catch (DateTimeParseException e) {
        AlertUtils.showError("Sai định dạng", "Giờ kết thúc phải có định dạng HH:mm");
        return;
      }
      LocalDateTime endTime = LocalDateTime.of(endDate, endTimePicker);

      if (!endTime.isAfter(startTime)) {
        AlertUtils.showError("Lỗi thời gian", "Thời gian kết thúc phải sau thời gian bắt đầu.");
        return;
      }

      long durationMinsLong = java.time.Duration.between(startTime, endTime).toMinutes();
      if (durationMinsLong <= 0) {
        AlertUtils.showError("Lỗi thời gian", "Thời gian diễn ra phải tối thiểu 1 phút.");
        return;
      }
      int durationMins = (int) durationMinsLong;

      CreateAuctionRequest request =
          new CreateAuctionRequest(
              name, desc, startPrice, stepPrice, type, durationMins, startTime);
      createButton = LoadingButton.fromEvent(event);
      setCreateLoading(true);
      requests.createAuction(request);
    } catch (NumberFormatException e) {
      AlertUtils.showError("Sai định dạng", "Giá phải là số");
    } catch (IOException e) {
      setCreateLoading(false);
      AlertUtils.showError("Lỗi", "Server không phản hồi");
    } catch (Exception e) {
      setCreateLoading(false);
      AlertUtils.showError("Lỗi", e.getMessage());
      e.printStackTrace();
    }
  }

  private void handleCreateAuctionResult(String message) {
    if (!createLoading) {
      return;
    }
    setCreateLoading(false);
    if (!isSuccessMessage(message)) {
      AlertUtils.showError("Tạo phiên thất bại", message);
      return;
    }

    // Nếu user có chọn ảnh, upload ngay sau khi tạo auction thành công
    // itemId lấy từ ItemStore (item mới nhất của seller)
    if (selectedImageFile != null) {
      // Lấy item mới nhất từ store để có itemId
      var user = UserManager.getInstance().getCurrentUser();
      ItemStore.getInstance().getItemsBySeller(user.getId()).stream()
          .max(java.util.Comparator.comparingInt(app.common.models.Item::getId))
          .ifPresent(
              item -> {
                try {
                  requests.uploadImage(item.getId(), selectedImageFile);
                } catch (IOException e) {
                  // Upload ảnh thất bại không block luồng chính
                  AlertUtils.showError("Upload ảnh thất bại", e.getMessage());
                }
              });
    }

    AlertUtils.showInfo("Tạo phiên", message);
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  private boolean isSuccessMessage(String message) {
    if (message == null) {
      return false;
    }
    String normalized = message.toLowerCase();
    return normalized.contains("ok") || normalized.contains("thành công");
  }

  private void setCreateLoading(boolean loading) {
    createLoading = loading;
    if (loading) {
      stopCreateLoading = LoadingButton.show(createButton);
    } else {
      stopCreateLoading.run();
      stopCreateLoading = () -> {};
    }
  }

  /** Member. */
  @FXML
  public void handleBack(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  @Override
  public void cleanup() {
    notifications.removeMessageListener(createAuctionListener);
    setCreateLoading(false);
  }

  @FXML
  public void handleChooseImage(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn ảnh vật phẩm");
    fileChooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"));

    // getWindow() lấy Stage hiện tại từ button
    File file = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
    if (file != null) {
      selectedImageFile = file;
      // Cắt ngắn tên file nếu quá dài để UI đẹp hơn
      String displayName =
          file.getName().length() > 30 ? file.getName().substring(0, 27) + "..." : file.getName();
      imageFileNameLabel.setText(displayName);
    }
  }
}
