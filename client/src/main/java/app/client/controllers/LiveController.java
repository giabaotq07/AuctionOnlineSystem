package app.client.controllers;

import app.client.Client;
import app.client.manager.AuctionNavigator;
import app.client.manager.NavigationManager;
import app.client.manager.UserSession;
import app.client.utils.AlertUtils;
import app.common.dto.AuctionData;
import app.common.dto.AuctionDetail;
import app.common.dto.AuctionDetailResponse;
import app.common.dto.AuctionResultRequest;
import app.common.dto.AuctionResultResponse;
import app.common.dto.PlaceBidRequest;
import app.common.dto.PlaceBidResponse;
import app.common.dto.SettleWalletRequest;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.PacketType;
import app.common.enums.View;
import app.common.models.PacketReq;
import app.common.models.User;
import app.common.models.Wallet;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** LiveController. */
public class LiveController implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(LiveController.class);
  private AuctionData auction;
  private long currentPrice;
  private AuctionDetail auctionDetail;
  @FXML private Label itemNameLabel;
  @FXML private Label startPriceLabel;
  @FXML private Label stepPriceLabel;
  @FXML private Label currentPriceLabel;
  @FXML private Label depositLabel;
  @FXML private Label timeLabel;
  @FXML private TextField bidAmountField;
  @FXML private TextArea description;
  @FXML private Label availableBalanceLabel;
  private ScheduledExecutorService scheduler;
  private boolean resultRequested = false;
  private boolean auctionClosedShown = false;
  private boolean cleanedUp = false;
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
  private boolean settlementSent = false;

  /** Member. */
  @FXML
  public void initialize() {
    updateAvailableBalance();
  }

  /** setAuction. */
  public void setAuction(AuctionDetail detail) {
    if (detail == null || detail.auction() == null) {
      return;
    }
    auctionDetail = detail;
    auction = detail.auction();
    currentPrice = auction.highestBid();
    applyDetail(detail);
  }

  private void handleDetailResponse(AuctionDetailResponse response) {
    if (auction == null) {
      return;
    }
    if (response == null) {
      return;
    }
    AuctionDetail detail = response.detail();
    if (detail == null && response.notModified()) {
      detail = AuctionNavigator.getInstance().getCachedDetail(response.auctionId());
    }
    if (detail == null || detail.auctionId() != auction.id()) {
      return;
    }
    AuctionNavigator.getInstance().cacheDetail(detail);
    auctionDetail = detail;
    auction = detail.auction();
    applyDetail(auctionDetail);
  }

  private void handleBidResponse(PlaceBidResponse response, String message) {
    if (response == null) {
      return;
    }
    if (auction == null || response.auctionId() != auction.id()) {
      return;
    }
    currentPrice = Math.max(currentPrice, response.highestBidAmount());
    currentPriceLabel.setText(formatCurrency(currentPrice));
    bidAmountField.clear();
    if (UserSession.getInstance().getCurrentUser() != null
        && response.bidderId() == UserSession.getInstance().getCurrentUser().getId()) {
      AlertUtils.showInfo("Thành công", message);
    }
  }

  private void handleWalletUpdate(WalletUpdateResponse response, boolean success, String message) {
    if (!success) {
      AlertUtils.showError("Ví", message);
      return;
    }
    updateAvailableBalance();
  }

  private void updateAvailableBalance() {
    updateAvailableBalance(UserSession.getInstance().getCurrentUser());
  }

  private void updateAvailableBalance(User user) {
    if (availableBalanceLabel == null) {
      return;
    }
    if (user == null) {
      availableBalanceLabel.setText("Số dư: 0 đ");
      return;
    }
    Wallet wallet = user.getWallet();
    BigDecimal available = wallet.getAvailableBalance();
    availableBalanceLabel.setText("Số dư khả dụng: " + formatCurrency(available) + " đ");
  }

  private String formatCurrency(BigDecimal amount) {
    if (amount == null) {
      return "0";
    }
    return currencyFormat.format(amount);
  }

  private String formatCurrency(long amount) {
    return String.format("%,d đ", amount);
  }

  private void handleAuctionResult(AuctionResultResponse response) {
    if (auction == null) {
      return;
    }
    if (response == null || response.auctionId() != auction.id()) {
      return;
    }
    onAuctionClosed(
        auctionDetail != null ? auctionDetail.itemName() : "",
        response.winner().name(),
        response.finalPrice());
    requestWalletSettlement();
  }

  private void requestWalletSettlement() {
    if (settlementSent || auction == null) {
      return;
    }
    if (UserSession.getInstance().getCurrentUser() == null) {
      return;
    }
    try {
      settlementSent = true;
      SettleWalletRequest request = new SettleWalletRequest(auction.id());
      Client.getInstance().sendRequest(PacketReq.of(PacketType.SETTLE_WALLET, request));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void applyDetail(AuctionDetail detail) {
    itemNameLabel.setText(detail.itemName());
    startPriceLabel.setText(formatCurrency(detail.startingPrice()));
    stepPriceLabel.setText(formatCurrency(detail.stepPrice()));
    currentPrice = Math.max(currentPrice, detail.auction().highestBid());
    currentPriceLabel.setText(formatCurrency(currentPrice));
    depositLabel.setText(formatCurrency((long) (detail.startingPrice() * 0.2)));
    description.setText(detail.description());
    startCountdownTimer(detail.endTime());
  }

  /** onNewBidPlaced. */
  public void onNewBidPlaced(String itemName, long newPrice, String bidderName) {
    Platform.runLater(
        () -> {
          currentPrice = Math.max(currentPrice, newPrice);
          currentPriceLabel.setText(formatCurrency(currentPrice));
        });
  }

  /** onAuctionClosed. */
  public void onAuctionClosed(String itemName, String winnerName, long finalPrice) {
    Platform.runLater(
        () -> {
          if (auctionClosedShown) {
            return;
          }
          auctionClosedShown = true;
          AlertUtils.showInfo(
              "Kết thúc",
              "Phiên đấu giá đã kết thúc. Người thắng: "
                  + winnerName
                  + " với giá: "
                  + formatCurrency(finalPrice));
          timeLabel.setText("Phiên đấu giá đã kết thúc!");
          timeLabel.setStyle(
              "-fx-text-fill: red;" + "-fx-font-weight: bold;" + "-fx-font-size: 14px;");
          if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
          }
        });
  }

  /** Member. */
  @FXML
  public void handlePlaceBid(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }
    if (auction == null) {
      AlertUtils.showError("Lỗi", "Phiên không trong thời gian đặt giá");
      return;
    }
    if (UserSession.getInstance().getCurrentUser() == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để trả giá!");
      return;
    }
    long bidAmount;
    try {
      bidAmount = Long.parseLong(bidAmountField.getText().trim());
    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi", "Giá đấu không hợp lệ");
      return;
    }
    if (bidAmount <= currentPrice) {
      AlertUtils.showError("Lỗi", "Giá đấu phải lớn hơn giá hiện tại");
      return;
    }
    BigDecimal available = UserSession.getInstance().getCurrentUser().getWallet().getAvailableBalance();
    if (available != null && available.compareTo(BigDecimal.valueOf(bidAmount)) < 0) {
      AlertUtils.showError("Lỗi", "Số dư khả dụng không đủ để đặt giá");
      return;
    }
    PlaceBidRequest request = new PlaceBidRequest(auction.id(), bidAmount);
    Client.getInstance().sendRequest(PacketReq.of(PacketType.PLACE_BID, request));
  }

  private void startCountdownTimer(LocalDateTime endTime) {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    // create a daemon thread so the JVM can exit when the UI is closed
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r);
              t.setDaemon(true);
              return t;
            });
    scheduler.scheduleAtFixedRate(
        () -> {
          Platform.runLater(
              () -> {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(endTime)) {
                  if (!resultRequested) {
                    resultRequested = true;
                    requestAuctionResult();
                  }
                  scheduler.shutdownNow();
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
    if (auction == null) {
      return;
    }
    try {
      AuctionResultRequest request = new AuctionResultRequest(auction.id());
      Client.getInstance().sendRequest(PacketReq.of(PacketType.FETCH_AUCTION_RESULT, request));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void updateCountdownLabel(LocalDateTime now, LocalDateTime endTime) {
    long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
    long days = totalSeconds / 86400;
    long hours = (totalSeconds % 86400) / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    timeLabel.setText(String.format("%d Ngày %02d:%02d:%02d", days, hours, minutes, seconds));
  }

  @Override
  public void cleanup() {
    if (cleanedUp) {
      return;
    }
    cleanedUp = true;
    logger.info("Cleaning up LiveController");
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    resultRequested = false;
    auctionClosedShown = false;
    settlementSent = false;
    auctionDetail = null;
    auction = null;
  }

  /** Member. */
  @FXML
  public void switchToUi(ActionEvent event) {
    cleanup();
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
