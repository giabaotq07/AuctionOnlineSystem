package app.controllers;

import app.dao.UserDAO;
import app.models.*;
import app.services.UserService;
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
  // ===== INPUT =====
  @FXML private ListView<AuctionSession> sessionListView;

  @FXML private TextField bidderField;
  @FXML private TextField amountField;

  @FXML private TextArea outputArea;

  private AuctionSession session;

  // ===== LOAD LIST =====
  @FXML
  public void initialize() {
    userDao = new UserDAO();
    userService = new UserService(userDao);
    sessionListView.getItems().clear();
    sessionListView.getItems().addAll(DataStore.sessions);

    System.out.println("Loaded sessions: " + DataStore.sessions.size());

    sessionListView.setOnMouseClicked(
        e -> {
          AuctionSession s = sessionListView.getSelectionModel().getSelectedItem();
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

      boolean success = session.placeBid(bidder, amount);
      if (success) {
        HistoryStore.history.add(
            new HistoryRecord(
                session.getId(),
                HistoryType.BID,
                bidder.getName() + " bid $" + amount + " vào " + session.getItem().getName()));
      }

      if (success) {
        outputArea.setText("Đặt giá thành công!");
      } else {
        outputArea.setText("Đặt giá thất bại!");
      }

    } catch (Exception e) {
      outputArea.setText("Lỗi dữ liệu!");
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
