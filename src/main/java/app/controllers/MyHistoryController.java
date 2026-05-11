package app.controllers;

import app.config.NavigationManager;
import app.data.AuctionSummary;
import app.data.HistoryRequest;
import app.data.HistoryResponse;
import app.data.Response;
import app.enums.PacketType;
import app.enums.View;
import app.models.Auction;
import app.models.PacketReq;
import app.models.User;
import app.network.Client;
import app.utils.AlertUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class MyHistoryController {

  // Khớp fx:id từ file FXML của ông
  @FXML private ScrollPane historyScrollPane;
  @FXML private HBox historyContainerPane;

  private static final double CARD_WIDTH = 280;
  private static final double SPACING = 20; // Khớp với spacing="20" trong FXML

  private final Client client = Client.getInstance();
  private final List<AuctionSummary> summaries = new ArrayList<>();
  private User currentUser = client.getCurrentUser();
  private Consumer<Response> historyHandler;

  @FXML
  public void initialize() {
    historyHandler =
        response ->
            Platform.runLater(
                () -> {
                  if (!(response instanceof HistoryResponse)) {
                    return;
                  }
                  HistoryResponse historyResponse = (HistoryResponse) response;
                  if (historyResponse.success() && historyResponse.auctions() != null) {
                    summaries.clear();
                    summaries.addAll(historyResponse.auctions());
                    refreshHistoryContainer();
                    startAutoScroll();
                  }
                });
    Client.getInstance().subscribe(PacketType.FETCH_HISTORY, historyHandler);

    requestHistory();
  }

  private void requestHistory() {
    if (currentUser == null) {
      return;
    }
    try {
      HistoryRequest request = new HistoryRequest(currentUser.getId());
      Client.getInstance().sendRequest(PacketReq.of(PacketType.FETCH_HISTORY, request));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void refreshHistoryContainer() {
    if (historyContainerPane == null) return;
    historyContainerPane.getChildren().clear();
    for (AuctionSummary summary : summaries) {
      historyContainerPane.getChildren().add(createAuctionCard(summary));
    }
  }

  private VBox createAuctionCard(AuctionSummary summary) {
    Auction session = summary.auction();
    VBox vbox = new VBox();
    vbox.setPrefWidth(CARD_WIDTH);
    vbox.setMinWidth(CARD_WIDTH);
    vbox.setMaxWidth(CARD_WIDTH);
    vbox.setStyle(
        "-fx-background-color: #1a1f35; -fx-background-radius: 8; -fx-padding: 15; -fx-spacing: 10;");

    Label badge =
        new Label(currentUser.getId() == session.getSellerId() ? "✪ ĐỒ CỦA TÔI" : "✔ ĐÃ THAM GIA");
    badge.setStyle(
        currentUser.getId() == session.getSellerId()
            ? "-fx-text-fill: #00ff88; -fx-font-weight: bold;"
            : "-fx-text-fill: #4caf50; -fx-font-weight: bold;");

    Label titleLabel = new Label(summary.itemName());
    titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
    titleLabel.setWrapText(true);

    long displayPrice = summary.currentPrice();
    Label priceLabel = new Label("Giá: " + displayPrice + "đ");
    priceLabel.setStyle("-fx-text-fill: #e91e63; -fx-font-weight: bold;");

    Button btnDetail = new Button("Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle("-fx-background-color: #673ab7; -fx-text-fill: white; -fx-cursor: hand;");
    btnDetail.setOnAction(e -> handleGoToLive(session));

    vbox.getChildren().addAll(badge, titleLabel, priceLabel, btnDetail);
    return vbox;
  }

  private void startAutoScroll() {
    if (historyScrollPane == null || historyContainerPane == null) return;

    Timeline scrollTimeline =
        new Timeline(
            new KeyFrame(
                Duration.seconds(3),
                e -> {
                  double contentWidth = historyContainerPane.getWidth();
                  double viewWidth = historyScrollPane.getViewportBounds().getWidth();
                  double maxScroll = contentWidth - viewWidth;

                  if (maxScroll <= 0) return;

                  double step = CARD_WIDTH + SPACING;
                  double nextPixel = (historyScrollPane.getHvalue() * maxScroll) + step;

                  if (nextPixel >= maxScroll + 10) { // Thêm tí đệm để reset mượt
                    nextPixel = 0;
                  }
                  historyScrollPane.setHvalue(nextPixel / maxScroll);
                }));
    scrollTimeline.setCycleCount(Timeline.INDEFINITE);
    scrollTimeline.play();

    historyScrollPane.setOnMouseEntered(ev -> scrollTimeline.pause());
    historyScrollPane.setOnMouseExited(ev -> scrollTimeline.play());
  }

  private void handleGoToLive(Auction session) {
    try {
      NavigationManager.getInstance()
          .navigateTo(
              View.LIVE,
              c -> {
                if (c instanceof LiveController) ((LiveController) c).setSession(session);
              });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  public void SwitchToUI() {
    if (historyHandler != null) {
      Client.getInstance().unsubscribe(PacketType.FETCH_HISTORY, historyHandler);
    }
    try {
      NavigationManager.getInstance().navigateTo(View.UI);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
