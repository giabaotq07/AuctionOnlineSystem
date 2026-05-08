package app.controllers;

import app.config.NavigationManager;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.enums.ItemType;
import app.enums.PacketType;
import app.enums.View;
import app.models.*;
import app.network.Client;
import app.service.AuctionService;
import app.service.ItemService;
import app.utils.AlertUtils;
import app.utils.JsonUtil;
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

  Client client = Client.getInstance();

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

    if (Client.getInstance().isConnected()) {
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
      long startPrice = Long.parseLong(startingPriceField.getText());
      long stepPrice = Long.parseLong(stepPriceField.getText());
      int durationMins = Integer.parseInt(durationField.getText());
      ItemType type = typeComboBox.getValue();

      if (name.isEmpty() || desc.isEmpty()) {
        AlertUtils.showError("Lỗi", "Thiếu thông tin.");
        return;
      }

      // ================== 1. SAVE ITEM ==================
      ItemService itemService = new ItemService(new MySqlItemDAO());
      Item item =
          ItemFactory.createItem(
              name, client.getCurrentUser().getId(), desc, startPrice, stepPrice, type);
      item = itemService.add(item);

      // ================== 2. SAVE AUCTION ==================
      Auction session =
          new Auction(
              item.getId(),
              DataStore.currentUser.getId(),
              LocalDateTime.now().plusMinutes(durationMins));

      AuctionService auctionService = new AuctionService(new MySqlAuctionDAO(), new MySqlBidDAO());
      session = auctionService.createAuction(session);

      // ================== 3. UI CACHE (optional) ==================
      DataStore.sessions.add(session);
      AuctionStateManager.getInstance().addSession(session);

      // ================== 4. NETWORK ==================
      Packet packet = new Packet(PacketType.CREATE_AUCTION, JsonUtil.toJsonElement(session));

      Client.getInstance().sendRequest(packet);

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
