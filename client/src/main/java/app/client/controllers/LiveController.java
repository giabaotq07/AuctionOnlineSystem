package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.LiveAuctionSessionStore;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.AuctionData;
import app.common.dto.AuctionDetail;
import app.common.enums.AuctionStatus;
import app.common.enums.View;
import app.common.models.Auction;
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
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static app.common.enums.AuctionStatus.FINISHED;
import static app.common.enums.AuctionStatus.OPEN;
import static app.common.enums.AuctionStatus.RUNNING;

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
  @FXML private Label statusLabel;
  @FXML private TextField bidAmountField;
  @FXML private TextArea description;
  @FXML private Label availableBalanceLabel;
  @FXML private ProgressIndicator detailLoadingIndicator;
  private ScheduledExecutorService scheduler;
  private boolean resultRequested = false;
  private boolean auctionClosedShown = false;
  private boolean cleanedUp = false;
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private boolean bidLoading;
  private Button bidButton;
  private Runnable stopBidLoading = () -> {};
  private boolean detailRequestInFlight;
  private Integer requestedAuctionId;
  private final Runnable updateListener = () -> Platform.runLater(this::handleUpdateNotification);
  private final Consumer<String> messageListener =
      message -> Platform.runLater(() -> handleMessageNotification(message));
  private boolean settlementSent = false;
  private AuctionStatus lastKnownStatus;

  /** Member. */
  @FXML
  public void initialize() {
    notifications.addUpdateListener(updateListener);
    notifications.addMessageListener(messageListener);
    updateAvailableBalance();
    loadSessionAuction();
    maybeRequestAuctionDetail();
  }

  /** setAuction. */
  public void setAuction(AuctionDetail detail) {
    if (detail == null || detail.auction() == null) {
      return;
    }
    auctionDetail = detail;
    auction = detail.auction();
    lastKnownStatus = auction.status();
    currentPrice = auction.highestBid();
    applyDetail(detail);
  }

  private void updateAvailableBalance() {
    updateAvailableBalance(UserManager.getInstance().getCurrentUser());
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
    if (wallet == null) {
      availableBalanceLabel.setText("Số dư khả dụng: 0 đ");
      return;
    }
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

  private void handleUpdateNotification() {
    loadSessionAuction();
    refreshDetailFromStore();
    maybeRequestAuctionDetail();
    updateAvailableBalance();
    updateTimer();
  }

  private void updateTimer() {
    if (auction != null && auction.status() == FINISHED) {
      timeLabel.setText("Đã kết thúc");
    }
  }

  private void loadSessionAuction() {
    AuctionDetail sessionDetail = LiveAuctionSessionStore.getInstance().getSelectedDetail();
    if (sessionDetail != null && sessionDetail.auction() != null) {
      setAuction(sessionDetail);
      return;
    }
    Integer auctionId = LiveAuctionSessionStore.getInstance().getSelectedAuctionId();
    if (auctionId == null) {
      return;
    }
    AuctionDetail cached = AuctionStore.getInstance().getAuctionDetail(auctionId);
    if (cached != null) {
      LiveAuctionSessionStore.getInstance().setSelectedDetail(cached);
      setAuction(cached);
      return;
    }
    showAwaitingAuctionDetail();
  }

  private void maybeRequestAuctionDetail() {
    Integer auctionId = LiveAuctionSessionStore.getInstance().getSelectedAuctionId();
    if (auctionId == null) {
      setDetailLoading(false);
      return;
    }
    AuctionDetail cached = AuctionStore.getInstance().getAuctionDetail(auctionId);
    if (cached != null) {
      LiveAuctionSessionStore.getInstance().setSelectedDetail(cached);
      setAuction(cached);
      detailRequestInFlight = false;
      requestedAuctionId = null;
      setDetailLoading(false);
      return;
    }
    if (detailRequestInFlight && auctionId.equals(requestedAuctionId)) {
      setDetailLoading(true);
      return;
    }
    if (!requests.isConnected()) {
      setDetailLoading(false);
      AlertUtils.showError("Mất kết nối", "Vui lòng kết nối lại!");
      return;
    }
    if (UserManager.getInstance().getCurrentUser() == null) {
      setDetailLoading(false);
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }
    try {
      detailRequestInFlight = true;
      requestedAuctionId = auctionId;
      setDetailLoading(true);
      requests.fetchAuctionDetail(auctionId, -1);
    } catch (IOException e) {
      detailRequestInFlight = false;
      requestedAuctionId = null;
      setDetailLoading(false);
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void setDetailLoading(boolean loading) {
    if (detailLoadingIndicator != null) {
      detailLoadingIndicator.setVisible(loading);
      detailLoadingIndicator.setManaged(loading);
    }
    if (loading) {
      showAwaitingAuctionDetail();
    }
  }

  private void handleMessageNotification(String message) {
    if (message == null || message.isBlank()) {
      return;
    }
    if (bidLoading) {
      handleBidResult(message);
      return;
    }
    if (detailRequestInFlight && !isSuccessMessage(message)) {
      detailRequestInFlight = false;
      requestedAuctionId = null;
      setDetailLoading(false);
      AlertUtils.showError("Lỗi", message);
      return;
    }
    if (resultRequested) {
      handleAuctionResultMessage(message);
    }
  }

  private void handleBidResult(String message) {
    setBidLoading(false);
    refreshDetailFromStore();
    bidAmountField.clear();
    if (!isSuccessMessage(message)) {
      AlertUtils.showError("Đặt giá", message);
      return;
    }
    AlertUtils.showInfo("Đặt giá", message);
  }

  private void handleAuctionResultMessage(String message) {
    resultRequested = false;
    refreshDetailFromStore();
    if (auction == null) {
      return;
    }
    Auction cachedAuction = AuctionStore.getInstance().getAuction(auction.id());
    if (cachedAuction != null && cachedAuction.getStatus() == FINISHED) {
      showAuctionClosed(message);
      return;
    }
    AlertUtils.showError("Kết thúc", message);
  }

  private boolean isSuccessMessage(String message) {
    String normalized = message == null ? "" : message.toLowerCase();
    return "ok".equals(normalized) || normalized.contains("thành công");
  }

  private void refreshDetailFromStore() {
    if (auction == null) {
      return;
    }
    AuctionDetail cachedDetail = AuctionStore.getInstance().getAuctionDetail(auction.id());
    if (cachedDetail != null) {
      auctionDetail = cachedDetail;
      auction = cachedDetail.auction();
      applyDetail(cachedDetail);
      detailRequestInFlight = false;
      requestedAuctionId = null;
      setDetailLoading(false);
      return;
    }
    Auction cachedAuction = AuctionStore.getInstance().getAuction(auction.id());
    if (cachedAuction != null) {
      currentPrice = Math.max(currentPrice, cachedAuction.getHighestBid());
      currentPriceLabel.setText(formatCurrency(currentPrice));
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
    updateStatusLabel(detail.auction().status());
    if (detail.auction().status() == OPEN && detail.startTime() != null) {
      startCountdownTimer(detail.startTime(), "Bắt đầu sau: ", false);
    } else if (detail.auction().status() == RUNNING && detail.endTime() != null) {
      startCountdownTimer(detail.endTime());
    } else {
      updateTimer();
    }
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

  private void showAuctionClosed(String message) {
    if (auctionClosedShown) {
      return;
    }
    auctionClosedShown = true;
    AlertUtils.showInfo("Kết thúc", message);
    timeLabel.setText("Phiên đấu giá đã kết thúc!");
    timeLabel.setStyle("-fx-text-fill: red;" + "-fx-font-weight: bold;" + "-fx-font-size: 14px;");
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
  }

  /** Member. */
  @FXML
  public void handlePlaceBid(ActionEvent event) {
    if (bidLoading) {
      return;
    }
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }
    if (auction == null) {
      AlertUtils.showError("Lỗi", "Phiên không trong thời gian đặt giá");
      return;
    }
    User currentUser = UserManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để trả giá!");
      return;
    }
    if (auction.status() == AuctionStatus.OPEN) {
      AlertUtils.showError("Thông báo", "Chưa đến thời gian đấu giá");
      return;
    }
    if (auction.status() != RUNNING) {
      AlertUtils.showError("Lỗi", "Phiên không trong thời gian đặt giá");
      return;
    }
    long bidAmount;
    try {
      bidAmount = Long.parseLong(bidAmountField.getText().trim());
    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi", "Giá đấu không hợp lệ");
      return;
    }
    long minimumBid = minimumBid();
    if (bidAmount < minimumBid) {
      AlertUtils.showError("Lỗi", "Giá đấu phải tối thiểu " + formatCurrency(minimumBid));
      return;
    }
    Wallet wallet = currentUser.getWallet();
    BigDecimal available = wallet == null ? BigDecimal.ZERO : wallet.getAvailableBalance();
    if (available.compareTo(BigDecimal.valueOf(bidAmount)) < 0) {
      AlertUtils.showError("Lỗi", "Số dư khả dụng không đủ để đặt giá");
      return;
    }
    try {
      bidButton = LoadingButton.fromEvent(event);
      setBidLoading(true);
      requests.placeBid(auction.id(), bidAmount);
    } catch (IOException e) {
      setBidLoading(false);
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private long minimumBid() {
    long stepPrice = auctionDetail == null ? 1L : Math.max(auctionDetail.stepPrice(), 1L);
    return currentPrice + stepPrice;
  }

  private void setBidLoading(boolean loading) {
    bidLoading = loading;
    if (loading) {
      stopBidLoading = LoadingButton.show(bidButton);
    } else {
      stopBidLoading.run();
      stopBidLoading = () -> {};
    }
  }

  private void startCountdownTimer(LocalDateTime endTime) {
    startCountdownTimer(endTime, "", true);
  }

  private void startCountdownTimer(LocalDateTime targetTime, String prefix, boolean requestResult) {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
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
                if (!now.isBefore(targetTime)) {
                  if (requestResult && !resultRequested) {
                    resultRequested = true;
                    showAwaitingServerConfirmation();
                  } else if (!requestResult) {
                    timeLabel.setText("Đang chờ bắt đầu...");
                  }
                  scheduler.shutdownNow();
                } else {
                  updateCountdownLabel(now, targetTime, prefix);
                }
              });
        },
        0,
        1,
        TimeUnit.SECONDS);
  }

  private void updateStatusLabel(AuctionStatus status) {
    handleStatusTransition(status);
    if (statusLabel == null || status == null) {
      return;
    }
    if (status == AuctionStatus.OPEN) {
      statusLabel.setText("Sắp đấu giá");
      statusLabel.setTextFill(javafx.scene.paint.Color.web("#22c55e"));
      return;
    }
    if (status == AuctionStatus.RUNNING || status == AuctionStatus.ENDING_SOON) {
      statusLabel.setText("Đang đấu giá");
      statusLabel.setTextFill(javafx.scene.paint.Color.web("#f97316"));
      return;
    }
    statusLabel.setText("Đã kết thúc");
    statusLabel.setTextFill(javafx.scene.paint.Color.web("#ef4444"));
  }

  private void handleStatusTransition(AuctionStatus newStatus) {
    if (newStatus == null) {
      return;
    }
    AuctionStatus previous = lastKnownStatus;
    lastKnownStatus = newStatus;
    if (previous == AuctionStatus.OPEN && newStatus == AuctionStatus.RUNNING) {
      AlertUtils.showInfo("Thông báo", "Phiên đấu giá đã bắt đầu");
    }
  }

  private void updateCountdownLabel(LocalDateTime now, LocalDateTime endTime) {
    updateCountdownLabel(now, endTime, "");
  }

  private void updateCountdownLabel(LocalDateTime now, LocalDateTime endTime, String prefix) {
    long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
    long days = totalSeconds / 86400;
    long hours = (totalSeconds % 86400) / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    timeLabel.setText(
        String.format("%s%d Ngày %02d:%02d:%02d", prefix, days, hours, minutes, seconds));
  }

  private void showAwaitingServerConfirmation() {
    timeLabel.setText("Đang chờ server xác nhận kết thúc phiên...");
    timeLabel.setStyle(
        "-fx-text-fill: #c77d00;" + "-fx-font-weight: bold;" + "-fx-font-size: 13px;");
  }

  private void showAwaitingAuctionDetail() {
    if (timeLabel == null) {
      return;
    }
    timeLabel.setText("Đang tải chi tiết phiên...");
    timeLabel.setStyle("-fx-text-fill: #9aa0b4;" + "-fx-font-size: 13px;");
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
    notifications.removeUpdateListener(updateListener);
    notifications.removeMessageListener(messageListener);
    if (requests.isConnected()) {
      try {
        requests.unwatchAuction();
      } catch (IOException e) {
        logger.debug("Failed to unwatch auction on cleanup", e);
      }
    }
    setBidLoading(false);
    resultRequested = false;
    auctionClosedShown = false;
    auctionDetail = null;
    auction = null;
    detailRequestInFlight = false;
    requestedAuctionId = null;
    setDetailLoading(false);
    lastKnownStatus = null;
  }

  /** Member. */
  @FXML
  public void switchToUi(ActionEvent event) {
    cleanup();
    LiveAuctionSessionStore.getInstance().clear();
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
