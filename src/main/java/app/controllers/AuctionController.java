package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.dao.AuctionDAO;
import app.dao.HistoryDAO;
import app.dao.ItemDAO;
import app.enums.CommandType;
import app.enums.HistoryType;
import app.enums.ItemType;
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
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tổ chức phiên đấu giá!");
      Platform.runLater(() -> NavigationManager.getInstance().navigateTo(View.LOGIN));
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
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server.");
      return;
    }

    if (DataStore.currentUser == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
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
        AlertUtils.showError("Lỗi", "Thiếu thông tin.");
        return;
      }

      // ================== 1. SAVE ITEM ==================
      Item item = ItemFactory.createItem(name, desc, startPrice, stepPrice, type);
      item = ItemDAO.getInstance().addItem(item);

      // ================== 2. SAVE AUCTION ==================
      Auction session = new Auction(
              0,
              item,
              DataStore.currentUser,
              LocalDateTime.now().plusMinutes(durationMins)
      );

      session = AuctionDAO.getInstance().addAuction(session);

      // ================== 3. UI CACHE (optional) ==================
      DataStore.sessions.add(session);
      AuctionStateManager.getInstance().addSession(session);

      // ================== 4. NETWORK ==================
      MessagePacket<Auction> packet =
              new MessagePacket<>(CommandType.CREATE_AUCTION, session);

      Client.getInstance().sendRequest(packet);

      // ================== 5. SAVE HISTORY ==================
      HistoryDAO.getInstance().addHistoryRecord(
              new HistoryRecord(
                      session.getId(),
                      HistoryType.ADD_ITEM,
                      item.getName() + " | Giá: " + item.getStartingPrice(),
                      LocalDateTime.now()
              )
      );

      AlertUtils.showInfo("OK", "Tạo phiên thành công");
      handleBack(event);

    } catch (NumberFormatException e) {
      AlertUtils.showError("Sai định dạng", "Giá / thời gian phải là số");
    } catch (Exception e) {
      AlertUtils.showError("Lỗi", e.getMessage());
      e.printStackTrace();
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}