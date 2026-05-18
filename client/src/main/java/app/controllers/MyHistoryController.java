package app.controllers;

import app.controllers.manager.NavigationManager;
import app.dto.AuctionHistoryResponse;
import app.dto.AuctionSummary;
import app.enums.AuctionStatus;
import app.enums.PacketType;
import app.enums.View;
import app.models.Auction;
import app.models.PacketReq;
import app.models.User;
import app.network.PacketListener;
import app.Client;
import app.utils.AlertUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** MyHistoryController. */
public class MyHistoryController implements Cleanable {

  private static final double CARD_WIDTH = 260;
  private static final double CARD_HEIGHT = 320;

  @FXML private FlowPane runningPane;

  @FXML private ComboBox<String> typeFilterComboBox;

  @FXML private FlowPane finishedPane;

  private final Client client = Client.getInstance();

  private final List<AuctionSummary> summaries = new ArrayList<>();

  private final User currentUser = client.getCurrentUser();

  private PacketListener<AuctionHistoryResponse> historyHandler;

  /** Member. */
  @FXML
  public void initialize() {
    typeFilterComboBox.getItems().addAll("ALL", "ELECTRONICS", "ART", "VEHICLE");

    typeFilterComboBox.setValue("ALL");

    typeFilterComboBox.setOnAction(e -> rebuildUi());

    historyHandler =
        (AuctionHistoryResponse response, boolean success, String message) ->
            Platform.runLater(
                () -> {
                  if (!success) {
                    AlertUtils.showError("Lỗi", message);
                    return;
                  }
                  if (response != null && response.auctions() != null) {

                    summaries.clear();

                    summaries.addAll(response.auctions());

                    rebuildUi();
                  }
                });

    client.subscribe(PacketType.FETCH_AUCTION_HISTORY, historyHandler);

    requestHistory();
  }

  private void requestHistory() {

    if (currentUser == null) {
      return;
    }

    try {

      client.sendRequest(PacketReq.of(PacketType.FETCH_AUCTION_HISTORY));

    } catch (IOException e) {

      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void rebuildUi() {

    if (runningPane == null || finishedPane == null) {
      return;
    }

    runningPane.getChildren().clear();

    finishedPane.getChildren().clear();

    for (AuctionSummary summary : summaries) {

      VBox card = createAuctionCard(summary);

      if (summary.status() == AuctionStatus.RUNNING) {

        runningPane.getChildren().add(card);

      } else {

        finishedPane.getChildren().add(card);
      }
    }
  }

  private VBox createAuctionCard(AuctionSummary summary) {

    final Auction auction = toAuction(summary);

    VBox vbox = new VBox();

    vbox.setPrefWidth(CARD_WIDTH);
    vbox.setMinWidth(CARD_WIDTH);
    vbox.setMaxWidth(CARD_WIDTH);

    vbox.setPrefHeight(CARD_HEIGHT);

    vbox.setStyle(
        "-fx-background-color: #1a1f35;"
            + "-fx-background-radius: 8;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);"
            + "-fx-padding: 15;"
            + "-fx-spacing: 10;");

    StackPane imagePane = new StackPane();

    imagePane.setPrefHeight(100);

    imagePane.setStyle("-fx-background-color: #2a2f45;" + "-fx-background-radius: 5;");

    Label imgLabel = new Label("Ảnh tài sản");

    imgLabel.setStyle("-fx-text-fill: #aaa;");

    imagePane.getChildren().add(imgLabel);

    Label badge =
        new Label(currentUser.getId() == auction.getSellerId() ? "✪ ĐỒ CỦA TÔI" : "✔ ĐÃ THAM GIA");

    badge.setStyle(
        currentUser.getId() == auction.getSellerId()
            ? "-fx-text-fill: #00ff88; -fx-font-weight: bold;"
            : "-fx-text-fill: #4caf50; -fx-font-weight: bold;");

    Label titleLabel = new Label(summary.itemName());

    titleLabel.setWrapText(true);

    titleLabel.setStyle(
        "-fx-font-weight: bold;" + "-fx-font-size: 14px;" + "-fx-text-fill: white;");

    Label priceLabel = new Label("Giá hiện tại: " + summary.currentPrice() + " đ");

    priceLabel.setStyle("-fx-text-fill: #e91e63;" + "-fx-font-weight: bold;");

    Label timeLabel = new Label("Kết thúc: " + auction.getEndTime());

    timeLabel.setStyle("-fx-text-fill: #9aa0b4;" + "-fx-font-size: 12px;");

    Button btnDetail =
        new Button(auction.getStatus() == AuctionStatus.FINISHED ? "Xem kết quả" : "Chi tiết");

    btnDetail.setMaxWidth(Double.MAX_VALUE);

    btnDetail.setStyle(
        "-fx-background-color: #673ab7;" + "-fx-text-fill: white;" + "-fx-cursor: hand;");

    btnDetail.setOnAction(e -> handleGoToLive(auction));

    vbox.getChildren().addAll(imagePane, badge, titleLabel, priceLabel, timeLabel, btnDetail);

    return vbox;
  }

  private Auction toAuction(AuctionSummary summary) {
    return new Auction(
        summary.auctionId(),
        summary.itemId(),
        summary.sellerId(),
        summary.winnerId(),
        summary.status(),
        summary.startTime(),
        summary.endTime(),
        summary.highestBid(),
        summary.extendedCount(),
        summary.version(),
        null,
        null);
  }

  private void handleGoToLive(Auction auction) {

    try {

      NavigationManager.getInstance()
          .navigateTo(
              View.LIVE,
              c -> {
                if (c instanceof LiveController) {

                  ((LiveController) c).setAuction(auction);
                }
              });

    } catch (Exception e) {

      e.printStackTrace();
    }
  }

  /** Member. */
  @FXML
  public void handleReload() {

    requestHistory();
  }

  /** Member. */
  @FXML
  public void switchToUi() {

    if (historyHandler != null) {

      client.unsubscribe(PacketType.FETCH_AUCTION_HISTORY, historyHandler);
    }

    try {

      NavigationManager.getInstance().navigateTo(View.UI);

    } catch (Exception e) {

      e.printStackTrace();
    }
  }

  @Override
  public void cleanup() {

    if (historyHandler != null) {

      client.unsubscribe(PacketType.FETCH_AUCTION_HISTORY, historyHandler);
    }
  }
}
