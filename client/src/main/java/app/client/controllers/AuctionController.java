package app.client.controllers;

import app.client.Client;
import app.client.manager.NavigationManager;
import app.client.utils.AlertUtils;
import app.common.dto.CreateAuctionRequest;
import app.common.dto.CreateAuctionResponse;
import app.common.enums.ItemType;
import app.common.enums.PacketType;
import app.common.enums.View;
import app.common.models.PacketReq;
import app.common.observer.PacketListener;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** AuctionController. */
public class AuctionController {
  @FXML private TextField nameField;
  @FXML private TextArea descriptionField;
  @FXML private TextField startingPriceField;
  @FXML private TextField stepPriceField;
  @FXML private ComboBox<ItemType> typeComboBox;
  @FXML private TextField durationField;
  @FXML private DatePicker startDatePicker;
  @FXML private TextField startTimeField;
  private PacketListener<CreateAuctionResponse> createAuctionHandler;

  /** Member. */
  @FXML
  public void initialize() {
    if (Client.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tổ chức phiên đấu giá!");
      Platform.runLater(() -> NavigationManager.getInstance().navigateTo(View.LOGIN));
      return;
    }
    if (typeComboBox != null) {
      typeComboBox.getItems().setAll(ItemType.values());
      typeComboBox.getSelectionModel().selectFirst();
    }
    createAuctionHandler =
        (CreateAuctionResponse response, boolean success, String message) ->
            Platform.runLater(
                () -> {
                  if (success && response != null) {
                    AlertUtils.showInfo("OK", message);
                    if (createAuctionHandler != null) {
                      Client.getInstance()
                          .unsubscribe(PacketType.CREATE_AUCTION, createAuctionHandler);
                    }
                    NavigationManager.getInstance().navigateTo(View.UI);
                  } else {
                    AlertUtils.showError("Lỗi", message);
                  }
                });
    Client.getInstance()
        .subscribe(PacketType.CREATE_AUCTION, CreateAuctionResponse.class, createAuctionHandler);
  }

  /** Member. */
  @FXML
  public void handleAdd(ActionEvent event) {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server.");
      return;
    }
    if (Client.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }
    try {
      String name = nameField.getText();
      String desc = descriptionField.getText();
      long startPrice = Long.parseLong(startingPriceField.getText());
      long stepPrice = Long.parseLong(stepPriceField.getText());
      int durationMins = Integer.parseInt(durationField.getText());
      ItemType type = typeComboBox.getValue();

      LocalDate startDate = startDatePicker.getValue();
      String timeStr = startTimeField.getText();
      if (name.isEmpty() || desc.isEmpty() || startDate == null || timeStr == null || timeStr.isEmpty()) {
        AlertUtils.showError("Lỗi", "Thiếu thông tin.");
        return;
      }

      LocalTime startTimePicker;
      try {
          startTimePicker = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
      } catch (DateTimeParseException e) {
          AlertUtils.showError("Sai định dạng", "Giờ bắt đầu phải có định dạng HH:mm");
          return;
      }
      LocalDateTime startTime = LocalDateTime.of(startDate, startTimePicker);

      CreateAuctionRequest request =
          new CreateAuctionRequest(name, desc, startPrice, stepPrice, type, durationMins, startTime);
      Client.getInstance().sendRequest(PacketReq.of(PacketType.CREATE_AUCTION, request));
    } catch (NumberFormatException e) {
      AlertUtils.showError("Sai định dạng", "Giá / thời gian phải là số");
    } catch (IOException e) {
      AlertUtils.showError("Lỗi", "Server không phản hồi");
    } catch (Exception e) {
      AlertUtils.showError("Lỗi", e.getMessage());
      e.printStackTrace();
    }
  }

  /** Member. */
  @FXML
  public void handleBack(ActionEvent event) {
    if (createAuctionHandler != null) {
      Client.getInstance().unsubscribe(PacketType.CREATE_AUCTION, createAuctionHandler);
    }
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
