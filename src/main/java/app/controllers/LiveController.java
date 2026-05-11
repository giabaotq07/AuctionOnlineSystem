package app.controllers;

import app.config.NavigationManager;
import app.data.AuctionDetail;
import app.data.AuctionDetailRequest;
import app.data.AuctionDetailResponse;
import app.data.AuctionResultRequest;
import app.data.AuctionResultResponse;
import app.data.PlaceBidRequest;
import app.data.PlaceBidResponse;
import app.enums.AuctionStatus;
import app.enums.PacketType;
import app.enums.View;
import app.models.Auction;
import app.models.DataStore;
import app.models.Packet;
import app.network.Client;
import app.observer.AuctionObserver;
import app.utils.AlertUtils;
import app.utils.JsonUtil;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class LiveController implements AuctionObserver {
  private Auction session;
  private AuctionDetail auctionDetail;
  private PlaceBidResponse placeBidResponse;
  private AuctionResultResponse auctionResultResponse;
  @FXML private Label itemNameLabel;
  @FXML private Label startPriceLabel;
  @FXML private Label stepPriceLabel;
  @FXML private Label currentPriceLabel;
  @FXML private Label depositLabel;
  @FXML private Label timeLabel;
  @FXML private TextField bidAmountField;
  @FXML private TextArea description;
  private ScheduledExecutorService scheduler;

  @FXML
  public void initialize() {
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
                      if (packet.getType() == PacketType.FETCH_AUCTION_DETAIL) {
                        AuctionDetailResponse response =
                            JsonUtil.fromJson(packet.getData(), AuctionDetailResponse.class);
                        if (response.success() && response.detail() != null) {
                          auctionDetail = response.detail();
                          applyDetail(auctionDetail);
                        }
                      }
                      if (packet.getType() == PacketType.FETCH_AUCTION_RESULT) {
                        auctionResultResponse =
                            JsonUtil.fromJson(packet.getData(), AuctionResultResponse.class);
                        if (auctionResultResponse.success()) {
                          onAuctionClosed(
                              auctionDetail != null ? auctionDetail.itemName() : "",
                              auctionResultResponse.winnerName(),
                              auctionResultResponse.finalPrice());
                        }
                      }
                    }));
  }

  public void setSession(Auction session) {
    this.session = session;
    try {
      AuctionDetailRequest request = new AuctionDetailRequest(session.getId());
      Packet packet = new Packet(PacketType.FETCH_AUCTION_DETAIL, JsonUtil.toJson(request));
      Client.getInstance().sendRequest(packet);
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void applyDetail(AuctionDetail detail) {
    if (itemNameLabel != null) itemNameLabel.setText(detail.itemName());
    if (startPriceLabel != null) startPriceLabel.setText("" + detail.startingPrice());
    if (stepPriceLabel != null) stepPriceLabel.setText("" + detail.stepPrice());

    if (currentPriceLabel != null) {
      currentPriceLabel.setText("" + detail.currentPrice());
    }

    if (depositLabel != null)
      depositLabel.setText(String.format("%,.0f đ", detail.startingPrice() * 0.2));

    if (description != null) {
      description.setText(detail.description());
    }

    startCountdownTimer(detail.endTime());
  }

  @Override
  public void onNewBidPlaced(String itemName, long newPrice, String bidderName) {
    Platform.runLater(
        () -> {
          if (currentPriceLabel != null) {
            currentPriceLabel.setText(newPrice + " đ");
          }
        });
  }

  @Override
  public void onAuctionClosed(String itemName, String winnerName, long finalPrice) {
    Platform.runLater(
        () -> {
          AlertUtils.showInfo(
              "Kết thúc",
              "Phiên đấu giá đã kết thúc. Người thắng: "
                  + winnerName
                  + " với giá: "
                  + finalPrice
                  + " đ");
          timeLabel.setText("Phiên đấu giá đã kết thúc!");
          timeLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
          if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
          }
        });
  }

  public void notifyUpdateBid() {
    if (placeBidResponse == null) {
      return;
    }
    int bidderId = placeBidResponse.bidderId();
    long highestBid = placeBidResponse.highestBidAmount();
    if (bidderId == Client.getInstance().getCurrentUser().getId()) {
      AlertUtils.showInfo("Thành công", "Đặt giá thành công!");
    }
    currentPriceLabel.setText("" + highestBid);
    bidAmountField.clear();
  }

  @FXML
  public void handlePlaceBid(ActionEvent event) throws IOException {
    if (!Client.getInstance().connected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }
    if (session == null || !session.isRunning()) {
      AlertUtils.showError("Lỗi", "Phiên không trong thời gian đặt giá");
      return;
    }

    if (DataStore.currentUser == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để trả giá!");
      return;
    }
    long bidAmount;
    long currentPrice;
    bidAmount = Long.parseLong(bidAmountField.getText());
    currentPrice = Long.parseLong(currentPriceLabel.getText());
    PlaceBidRequest request =
        new PlaceBidRequest(
            session.getId(), DataStore.currentUser.getId(), bidAmount, currentPrice);
    Packet packet = new Packet(PacketType.PLACE_BID, JsonUtil.toJson(request));
    Client.getInstance().sendRequest(packet);
  }

  private void startCountdownTimer(LocalDateTime endTime) {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        () -> {
          Platform.runLater(
              () -> {
                LocalDateTime now = LocalDateTime.now();

                if (now.isAfter(endTime)) {
                  scheduler.shutdown();
                  if (session != null && session.isRunning()) {
                    session.setStatus(AuctionStatus.FINISHED);
                  }
                  requestAuctionResult();
                } else {
                  updateCountdownLabel(now, endTime);
                }
              });
        },
        0,
        1,
        TimeUnit.SECONDS);
  }

  private void requestAuctionResult() {
    if (session == null) {
      return;
    }
    try {
      AuctionResultRequest request = new AuctionResultRequest(session.getId());
      Packet packet = new Packet(PacketType.FETCH_AUCTION_RESULT, JsonUtil.toJson(request));
      Client.getInstance().sendRequest(packet);
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void updateCountdownLabel(LocalDateTime now, LocalDateTime endTime) {
    long days = ChronoUnit.DAYS.between(now, endTime);
    LocalDateTime temp = now.plusDays(days);
    long hours = ChronoUnit.HOURS.between(temp, endTime);
    temp = temp.plusHours(hours);
    long minutes = ChronoUnit.MINUTES.between(temp, endTime);
    temp = temp.plusMinutes(minutes);
    long seconds = ChronoUnit.SECONDS.between(temp, endTime);

    timeLabel.setText(String.format("%d Ngày %02d:%02d:%02d", days, hours, minutes, seconds));
  }

  @FXML
  public void SwitchToUI(ActionEvent event) {
    if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdown();
    if (session != null) session.removeObserver(this);
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
