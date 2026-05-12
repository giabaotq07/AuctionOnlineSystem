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
import app.models.PacketReq;
import app.network.Client;
import app.network.PacketListener;
import app.observer.AuctionObserver;
import app.utils.AlertUtils;
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
  private PacketListener<PlaceBidResponse> placeBidHandler;
  private PacketListener<AuctionDetailResponse> auctionDetailHandler;
  private PacketListener<AuctionResultResponse> auctionResultHandler;
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

    placeBidHandler =
            (PlaceBidResponse response) ->
                    Platform.runLater(
                            () -> {
                              placeBidResponse = response;
                              notifyUpdateBid();
                            });

    Client.getInstance()
            .subscribe(PacketType.PLACE_BID, placeBidHandler);

    auctionDetailHandler =
            (AuctionDetailResponse response) ->
                    Platform.runLater(
                            () -> {

                              if (response.success()
                                      && response.detail() != null) {

                                auctionDetail = response.detail();

                                applyDetail(auctionDetail);
                              }
                            });

    Client.getInstance()
            .subscribe(
                    PacketType.FETCH_AUCTION_DETAIL,
                    auctionDetailHandler);

    auctionResultHandler =
            (AuctionResultResponse response) ->
                    Platform.runLater(
                            () -> {

                              auctionResultResponse = response;

                              if (response.success()) {

                                onAuctionClosed(
                                        auctionDetail != null
                                                ? auctionDetail.itemName()
                                                : "",
                                        response.winnerName(),
                                        response.finalPrice());
                              }
                            });

    Client.getInstance()
            .subscribe(
                    PacketType.FETCH_AUCTION_RESULT,
                    auctionResultHandler);
  }

  public void setSession(Auction session) {
    this.session = session;
    try {
      AuctionDetailRequest request = new AuctionDetailRequest(session.getId());
      Client.getInstance().sendRequest(PacketReq.of(PacketType.FETCH_AUCTION_DETAIL, request));
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

    if (Client.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để trả giá!");
      return;
    }
    long bidAmount;
    long currentPrice;
    bidAmount = Long.parseLong(bidAmountField.getText());
    currentPrice = Long.parseLong(currentPriceLabel.getText());
    PlaceBidRequest request =
            new PlaceBidRequest(
                    session.getId(),
                    Client.getInstance().getCurrentUser().getId(),
                    bidAmount,
                    currentPrice);
    Client.getInstance().sendRequest(PacketReq.of(PacketType.PLACE_BID, request));
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
      Client.getInstance().sendRequest(PacketReq.of(PacketType.FETCH_AUCTION_RESULT, request));
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
    if (placeBidHandler != null) {
      Client.getInstance().unsubscribe(PacketType.PLACE_BID, placeBidHandler);
    }
    if (auctionDetailHandler != null) {
      Client.getInstance().unsubscribe(PacketType.FETCH_AUCTION_DETAIL, auctionDetailHandler);
    }
    if (auctionResultHandler != null) {
      Client.getInstance().unsubscribe(PacketType.FETCH_AUCTION_RESULT, auctionResultHandler);
    }
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}