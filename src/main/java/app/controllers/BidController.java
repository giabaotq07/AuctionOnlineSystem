package app.controllers;

import app.config.NavigationManager;
import app.dao.UserDAO;
import app.dao.impl.*;
import app.enums.HistoryType;
import app.enums.View;
import app.models.*;
import app.network.Client;
import app.service.BidService;
import app.service.ItemService;
import app.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class BidController {

  UserDAO userDao;
  UserService userService;
  // DAO / Service để xử lý bid và lịch sử
  private BidService bidService;
  private ItemService itemService;
  // ===== INPUT =====
  @FXML private ListView<Auction> sessionListView;

  @FXML private TextField bidderField;
  @FXML private TextField amountField;

  @FXML private TextArea outputArea;

  private Auction session;

  // ===== LOAD LIST =====
  @FXML
  public void initialize() {
    userService = new UserService(new MySqlUserDAO());
    itemService = new ItemService(new MySqlItemDAO());
    // Khởi tạo service/dao cần thiết
    bidService = new BidService(new MySqlBidDAO(), new MySqlAutoBidDAO(), new MySqlAuctionDAO());
    sessionListView.getItems().clear();
    sessionListView.getItems().addAll(DataStore.sessions);

    System.out.println("Loaded sessions: " + DataStore.sessions.size());

    sessionListView.setOnMouseClicked(
        e -> {
          Auction s = sessionListView.getSelectionModel().getSelectedItem();
          Item item;
          if (itemService.getById(s.getItemId()).isPresent()) {
            item = itemService.getById(s.getItemId()).get();
            outputArea.setText("Item: " + item.getName() + "\nGiá: $" + s.getHighestBid());
          } else {
            outputArea.setText("Item: " + "ko có" + "\nGiá: $" + s.getHighestBid());
          }
        });
  }

  // ===== BID =====
  @FXML
  public void handleBid() {
    try {
      if (session == null) {
        outputArea.setText("Chưa chọn session!");
        return;
      }

      String userName = bidderField.getText();
      long amount = Long.parseLong(amountField.getText());
      User bidder = Client.getInstance().getCurrentUser();
      String itemName;
      if (itemService.getById(session.getItemId()).isPresent()) {
        itemName = itemService.getById(session.getItemId()).get().getName();
      } else {
        itemName = "ko có";
      }
      try {
        bidService.placeBid(session.getId(), bidder.getId(), amount);

        // ✅ CẬP NHẬT OUTPUT NGAY LẬP TỨC từ dữ liệu trong database
        bidService
            .getHighestBid(session.getId())
            .ifPresent(
                highestBid ->
                    outputArea.setText(
                        "Item: "
                            + itemName
                            + "\nGiá hiện tại: "
                            + highestBid.getAmount()
                            + "\nNguời trả giá cao nhất: "
                            + highestBid.getBidderName()));

        // Lưu lịch sử vào database
        HistoryRecord record =
            new HistoryRecord(
                session.getId(),
                HistoryType.BID,
                bidder.getName() + " bid $" + amount + " vào " + itemName);

        amountField.clear();
        bidderField.clear();
      } catch (Exception e) {
        outputArea.setText("Lỗi đặt giá: " + e.getMessage());
      }

    } catch (NumberFormatException e) {
      outputArea.setText("Lỗi dữ liệu! Vui lòng nhập số hợp lệ.");
    } catch (Exception e) {
      outputArea.setText("Lỗi: " + e.getMessage());
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
