package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.models.*;
import app.network.Client;
import java.time.LocalDateTime;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AuctionController {

  @FXML private TextField nameField;
  @FXML private TextArea descriptionField;
  @FXML private TextField startingPriceField;
  @FXML private TextField stepPriceField;
  @FXML private ComboBox<ItemType> typeComboBox;
  @FXML private TextField durationField;

  @FXML
  public void initialize() {
    if (DataStore.currentUser == null) {
      // Force them out right away if they somehow load this page without logging in
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tổ chức phiên đấu giá!");
      Platform.runLater(
          () -> {
            NavigationManager.getInstance().navigateTo(View.LOGIN);
          });
      return;
    }
    if (typeComboBox != null) {
      typeComboBox.getItems().setAll(ItemType.values());
      typeComboBox.getSelectionModel().selectFirst();
    }
  }

  @FXML
  public void handleAdd(ActionEvent event) {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
      return;
    }
    if (DataStore.currentUser == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tổ chức phiên đấu giá!");
      app.config.NavigationManager.getInstance().navigateTo(app.config.View.LOGIN);
      return;
    }

    try {
      String name = nameField.getText();
      String desc = descriptionField.getText();
      double startPrice = Double.parseDouble(startingPriceField.getText());
      double stepPrice = Double.parseDouble(stepPriceField.getText());
      int durationMins = Integer.parseInt(durationField.getText());
      ItemType type = typeComboBox.getValue();

      if (name.isEmpty() || desc.isEmpty()) {
        AlertUtils.showError("Lỗi", "Vui lòng nhập đầy đủ thông tin.");
        return;
      }

      int nextId = DataStore.sessions.size() + 1;
      Item item = ItemFactory.createItem(nextId, name, desc, startPrice, stepPrice, type);

      AuctionSession session =
          new AuctionSession(
              nextId, item, DataStore.currentUser, LocalDateTime.now().plusMinutes(durationMins));

      DataStore.sessions.add(session);
      app.models.AuctionStateManager.getInstance().addSession(session);

      // --- NEW CODE: BT U BROADCAST SANG CC CLIENT KHC ---
      app.models.MessagePacket<app.models.AuctionSession> createPacket =
          new app.models.MessagePacket<>(app.models.CommandType.CREATE_AUCTION, session);
      app.network.Client.getInstance().sendRequest(createPacket);
      // --- END NEW CODE ---

      HistoryStore.history.add(
          new HistoryRecord(
              session.getId(),
              HistoryType.ADD_ITEM,
              item.getName() + " Giá: " + item.getStartingPrice()));

      AlertUtils.showInfo("Thành công", "Phiên đấu giá đã được thêm thành công!");
      handleBack(event); // Redirect to FirstScene

    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi nhập liệu", "Giá và thời gian phải là số hợp lệ!");
    } catch (Exception e) {
      AlertUtils.showError("Lỗi", "Có lỗi xảy ra: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
