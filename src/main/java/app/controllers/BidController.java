package app.controllers;

import app.dao.UserDAO;
import app.enums.HistoryType;
import app.models.*;
import app.service.UserService;
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
  @FXML private ListView<Auction> sessionListView;

  @FXML private TextField bidderField;
  @FXML private TextField amountField;

  @FXML private TextArea outputArea;

  private Auction session;

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
          Auction s = sessionListView.getSelectionModel().getSelectedItem();
          if (s == null) return;
          session = s;

          outputArea.setText(
              "Item: " + s.getItemId() + "\nGiá: $" + s.getHighestBid());
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

      User bidder = userService.getUserByAccount(userName);
      boolean success;
      try {
        session.updateHighestBid(amount, bidder.getId());
        success = true;
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
        success = false;
      }
      if (success) {
        HistoryStore.history.add(
            new HistoryRecord(
                session.getId(),
                HistoryType.BID,
                bidder.getName() + " bid $" + amount + " vào " + session.getItemId()));
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
