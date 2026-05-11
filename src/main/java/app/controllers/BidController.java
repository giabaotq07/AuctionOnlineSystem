package app.controllers;

import app.config.NavigationManager;
import app.data.PlaceBidRequest;
import app.data.PlaceBidResponse;
import app.enums.PacketType;
import app.enums.View;
import app.models.Auction;
import app.models.DataStore;
import app.models.Packet;
import app.models.User;
import app.network.Client;
import app.utils.JsonUtil;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class BidController {

  // ===== INPUT =====
  @FXML private ListView<Auction> sessionListView;

  @FXML private TextField bidderField;
  @FXML private TextField amountField;

  @FXML private TextArea outputArea;

  private Auction session;
  private PlaceBidResponse placeBidResponse;

  // ===== LOAD LIST =====
  @FXML
  public void initialize() {
    sessionListView.getItems().clear();
    sessionListView.getItems().addAll(DataStore.sessions);

    Client.getInstance()
        .setOnMessageReceived(
            packet ->
                Platform.runLater(
                    () -> {
                      if (packet.getType() == PacketType.PLACE_BID) {
                        placeBidResponse =
                            JsonUtil.fromJson(packet.getData(), PlaceBidResponse.class);
                        notifyUpdateBid();
                      }
                    }));

    sessionListView.setOnMouseClicked(
        e -> {
          Auction s = sessionListView.getSelectionModel().getSelectedItem();
          if (s == null) {
            return;
          }
          session = s;
          outputArea.setText("Session: " + s.getId() + "\nGiá hiện tại: " + s.getHighestBid());
        });
  }

  private void notifyUpdateBid() {
    if (placeBidResponse == null) {
      return;
    }
    outputArea.setText(
        "Item: "
            + placeBidResponse.itemName()
            + "\nGiá hiện tại: "
            + placeBidResponse.highestBidAmount()
            + "\nNguời trả giá cao nhất: "
            + placeBidResponse.bidderName());
    amountField.clear();
    bidderField.clear();
  }

  // ===== BID =====
  @FXML
  public void handleBid() {
    try {
      if (session == null) {
        outputArea.setText("Chưa chọn session!");
        return;
      }

      long amount = Long.parseLong(amountField.getText());
      User bidder = Client.getInstance().getCurrentUser();
      if (bidder == null) {
        outputArea.setText("Bạn phải đăng nhập trước!");
        return;
      }

      PlaceBidRequest request =
          new PlaceBidRequest(session.getId(), bidder.getId(), amount, session.getHighestBid());
      Client.getInstance().sendRequest(new Packet(PacketType.PLACE_BID, JsonUtil.toJson(request)));

    } catch (NumberFormatException e) {
      outputArea.setText("Lỗi dữ liệu! Vui lòng nhập số hợp lệ.");
    } catch (IOException e) {
      outputArea.setText("Lỗi kết nối: Server không phản hồi.");
    } catch (Exception e) {
      outputArea.setText("Lỗi: " + e.getMessage());
    }
  }

  @FXML
  public void handleBack(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
