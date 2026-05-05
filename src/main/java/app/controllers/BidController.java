package app.controllers;

import app.dao.UserDAO;
import app.dao.BidDAO;
import app.dao.HistoryDAO;
import app.enums.HistoryType;
import app.models.*;
import app.service.UserService;
import app.service.BidService;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class BidController {

  UserDAO userDao;
  UserService userService;
  // DAO / Service để xử lý bid và lịch sử
  private BidService bidService;
  private HistoryDAO historyDAO;
  // ===== INPUT =====
  @FXML private ListView<Auction> sessionListView;

  @FXML private TextField bidderField;
  @FXML private TextField amountField;

  @FXML private TextArea outputArea;

  private Auction session;

  // ===== LOAD LIST =====
  @FXML
  public void initialize() {
    userDao = UserDAO.getInstance();
    userService = new UserService(userDao);
    // Khởi tạo service/dao cần thiết
    bidService = new BidService(BidDAO.getInstance());
    historyDAO = HistoryDAO.getInstance();
    sessionListView.getItems().clear();
    sessionListView.getItems().addAll(DataStore.sessions);

    System.out.println("Loaded sessions: " + DataStore.sessions.size());

    sessionListView.setOnMouseClicked(
        e -> {
          Auction s = sessionListView.getSelectionModel().getSelectedItem();
          if (s == null) return;
          session = s;

          outputArea.setText(
              "Item: " + s.getItem().getName() + "\nGiá: $" + s.getCurrentHighestPrice());
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
      double amount = Double.parseDouble(amountField.getText());

      User bidder = userService.getUserByAccount(userName);
      if (bidder == null) {
        outputArea.setText("Người dùng không tồn tại!");
        return;
      }

      try {
        bidService.placeBid(session.getId(), bidder.getId(), amount);

        // ✅ CẬP NHẬT OUTPUT NGAY LẬP TỨC từ dữ liệu trong database
        BidTransaction highestBid = bidService.getHighestBid(session.getId());
        if (highestBid != null) {
          outputArea.setText(
              "Item: " + session.getItem().getName()
              + "\nGiá hiện tại: $" + String.format("%.2f", highestBid.getAmount())
              + "\nNguời trả giá cao nhất: " + highestBid.getBidder().getName());
        }

        // Lưu lịch sử vào database
        HistoryRecord record = new HistoryRecord(
            session.getId(),
            HistoryType.BID,
            bidder.getName() + " bid $" + amount + " vào " + session.getItem().getName());
        historyDAO.addHistoryRecord(record);

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
  public void handleBack(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/FirstScene.fxml"));
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Scene scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }
}
