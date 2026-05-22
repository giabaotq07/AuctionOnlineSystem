package app.client.controllers;

import static app.common.enums.AuctionStatus.FINISHED;
import static app.common.enums.AuctionStatus.OPEN;
import static app.common.enums.AuctionStatus.RUNNING;

import app.client.manager.AuctionDetailProxy;
import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.LiveAuctionSessionStore;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.store.ItemStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.AuctionPreview;
import app.common.enums.AuctionStatus;
import app.common.enums.View;
import app.common.models.Auction;
import app.common.models.Bid;
import app.common.models.Item;
import app.common.models.User;
import app.common.models.Wallet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** LiveController. */
public class LiveController implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(LiveController.class);
  private AuctionDetailProxy auctionProxy;
  private AuctionPreview preview;
  private Auction auction;
  private long currentPrice;
  @FXML private Label itemNameLabel;
  @FXML private Label startPriceLabel;
  @FXML private Label stepPriceLabel;
  @FXML private Label currentPriceLabel;
  @FXML private Label depositLabel;
  @FXML private Label timeLabel;
  @FXML private Label statusLabel;
  @FXML private TextField bidAmountField;
  @FXML private TextArea description;
  @FXML private TextArea bidHistoryArea;
  @FXML private Label availableBalanceLabel;
  @FXML private ProgressIndicator detailLoadingIndicator;
  @FXML private ImageView itemImageView;
  @FXML private Label imagePlaceholderLabel;
  private final Set<Integer> imageFetchInFlight = ConcurrentHashMap.newKeySet();
  private ScheduledExecutorService scheduler;
  private boolean resultRequested = false;
  private boolean auctionClosedShown = false;
  private boolean cleanedUp = false;
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
  private final DateTimeFormatter bidTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM");
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
  public void setAuction(Auction detail) {
    if (detail == null) {
      return;
    }
    auction = detail;
    preview = AuctionStore.getInstance().getPreview(detail.getId());
    lastKnownStatus = auction.getStatus();
    currentPrice = auction.getHighestBid();
    applyDetail(detail);
  }

  private void setPreview(AuctionPreview nextPreview) {
    if (nextPreview == null) {
      return;
    }
    preview = nextPreview;
    if (auction == null || auction.getId() != nextPreview.auctionId()) {
      applyPreview(nextPreview);
    }
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
    if (selectedStatus() == FINISHED) {
      timeLabel.setText("Đã kết thúc");
    }
  }

  private void loadSessionAuction() {
    auctionProxy = LiveAuctionSessionStore.getInstance().getSelectedProxy();
    if (auctionProxy == null) {
      showAwaitingAuctionDetail();
      return;
    }
    setPreview(auctionProxy.getPreview());
    Auction cached = auctionProxy.getDetailIfLoaded();
    if (cached != null) {
      setAuction(cached);
      detailRequestInFlight = false;
      requestedAuctionId = null;
      setDetailLoading(false);
      return;
    }
    showAwaitingBidHistory();
  }

  private void maybeRequestAuctionDetail() {
    auctionProxy = LiveAuctionSessionStore.getInstance().getSelectedProxy();
    if (auctionProxy == null) {
      setDetailLoading(false);
      return;
    }
    int auctionId = auctionProxy.getAuctionId();
    if (!auctionProxy.needsDetailRefresh()) {
      Auction detail = auctionProxy.getDetailIfLoaded();
      if (detail != null) {
        setAuction(detail);
      }
      detailRequestInFlight = false;
      requestedAuctionId = null;
      setDetailLoading(false);
      return;
    }
    if (auctionProxy.isRequestInFlight()) {
      detailRequestInFlight = true;
      requestedAuctionId = auctionId;
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
      auctionProxy.requestDetail();
      detailRequestInFlight = auctionProxy.isRequestInFlight();
      requestedAuctionId = auctionId;
      setDetailLoading(detailRequestInFlight);
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
    if (loading && auction == null && preview == null) {
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
    if (selectedStatus() == FINISHED) {
      showAuctionClosed(message);
      return;
    }
    AlertUtils.showInfo("Kết thúc", message);
  }

  private boolean isSuccessMessage(String message) {
    String normalized = message == null ? "" : message.toLowerCase();
    return "ok".equals(normalized) || normalized.contains("thành công");
  }

  private void refreshDetailFromStore() {
    AuctionDetailProxy selectedProxy = LiveAuctionSessionStore.getInstance().getSelectedProxy();
    if (selectedProxy != null) {
      auctionProxy = selectedProxy;
      setPreview(selectedProxy.getPreview());
      Auction detail = selectedProxy.getDetailIfLoaded();
      if (detail != null) {
        auction = detail;
        applyDetail(detail);
        detailRequestInFlight = false;
        requestedAuctionId = null;
        setDetailLoading(false);
        return;
      }
    }
    if (auction == null) {
      return;
    }
    Auction cachedDetail = AuctionStore.getInstance().getDetailIfLoaded(auction.getId());
    if (cachedDetail != null) {
      auction = cachedDetail;
      applyDetail(cachedDetail);
      detailRequestInFlight = false;
      requestedAuctionId = null;
      setDetailLoading(false);
      return;
    }
    AuctionPreview cachedPreview = AuctionStore.getInstance().getPreview(auction.getId());
    if (cachedPreview != null) {
      currentPrice = Math.max(currentPrice, cachedPreview.highestBid());
      currentPriceLabel.setText(formatCurrency(currentPrice));
    }
  }

  private void applyPreview(AuctionPreview preview) {
    itemNameLabel.setText(
        preview.itemName() == null ? "(Không có tên tài sản)" : preview.itemName());
    startPriceLabel.setText(formatCurrency(preview.startingPrice()));
    stepPriceLabel.setText(formatCurrency(Math.max(preview.stepPrice(), 1L)));
    currentPrice = Math.max(currentPrice, preview.highestBid());
    currentPriceLabel.setText(formatCurrency(currentPrice));
    depositLabel.setText(formatCurrency((long) (preview.startingPrice() * 0.2)));
    description.setText("Đang tải mô tả chi tiết...");
    showAwaitingBidHistory();
    updateStatusLabel(preview.status());
    if (preview.status() == OPEN && preview.startTime() != null) {
      startCountdownTimer(preview.startTime(), "Bắt đầu sau: ", false);
    } else if (preview.status() == RUNNING && preview.endTime() != null) {
      startCountdownTimer(preview.endTime());
    } else {
      updateTimer();
    }
    handleItemImage(preview.itemId(), preview.imageUrl());
  }

  private void applyDetail(Auction detail) {
    Item item = detail.getItem();
    long startingPrice = item == null ? detail.getHighestBid() : item.getStartingPrice();
    long stepPrice = item == null ? 1L : item.getStepPrice();
    itemNameLabel.setText(item == null ? "(Không có tên tài sản)" : item.getName());
    startPriceLabel.setText(formatCurrency(startingPrice));
    stepPriceLabel.setText(formatCurrency(stepPrice));
    currentPrice = Math.max(currentPrice, detail.getHighestBid());
    currentPriceLabel.setText(formatCurrency(currentPrice));
    depositLabel.setText(formatCurrency((long) (startingPrice * 0.2)));
    description.setText(item == null ? "" : item.getDescription());
    updateBidHistory(detail);
    updateStatusLabel(detail.getStatus());
    if (detail.getStatus() == OPEN && detail.getStartTime() != null) {
      startCountdownTimer(detail.getStartTime(), "Bắt đầu sau: ", false);
    } else if (detail.getStatus() == RUNNING && detail.getEndTime() != null) {
      startCountdownTimer(detail.getEndTime());
    } else {
      updateTimer();
    }
    handleItemImage(item);
  }

  private void updateBidHistory(Auction detail) {
    if (bidHistoryArea == null) {
      return;
    }
    if (detail == null || detail.getBids().isEmpty()) {
      bidHistoryArea.setText("Chưa có lượt đặt giá.");
      return;
    }
    StringBuilder builder = new StringBuilder();
    for (Bid bid : detail.getBids()) {
      if (bid == null) {
        continue;
      }
      String timeText = bid.getCreateAt() == null ? "" : bidTimeFormat.format(bid.getCreateAt());
      builder
          .append(timeText)
          .append(" - ")
          .append(
              bid.getBidderName() == null ? "Bidder #" + bid.getBidderId() : bid.getBidderName())
          .append(": ")
          .append(formatCurrency(bid.getAmount()));
      if (bid.isAutoBid()) {
        builder.append(" (auto)");
      }
      builder.append(System.lineSeparator());
    }
    bidHistoryArea.setText(builder.toString().stripTrailing());
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
    int selectedAuctionId = selectedAuctionId();
    if (selectedAuctionId <= 0) {
      AlertUtils.showError("Lỗi", "Phiên không trong thời gian đặt giá");
      return;
    }
    User currentUser = UserManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để trả giá!");
      return;
    }
    AuctionStatus selectedStatus = selectedStatus();
    if (selectedStatus == AuctionStatus.OPEN) {
      AlertUtils.showError("Thông báo", "Chưa đến thời gian đấu giá");
      return;
    }
    if (selectedStatus != RUNNING) {
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
      requests.placeBid(selectedAuctionId, bidAmount);
    } catch (IOException e) {
      setBidLoading(false);
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private long minimumBid() {
    Item item = auction == null ? null : auction.getItem();
    long previewStepPrice = preview == null ? 1L : Math.max(preview.stepPrice(), 1L);
    long stepPrice = item == null ? previewStepPrice : Math.max(item.getStepPrice(), 1L);
    return currentPrice + stepPrice;
  }

  private int selectedAuctionId() {
    if (auction != null) {
      return auction.getId();
    }
    if (preview != null) {
      return preview.auctionId();
    }
    return auctionProxy == null ? 0 : auctionProxy.getAuctionId();
  }

  private AuctionStatus selectedStatus() {
    if (auction != null) {
      return auction.getStatus();
    }
    return preview == null ? null : preview.status();
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
    if (status == AuctionStatus.RUNNING) {
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

  private void showAwaitingBidHistory() {
    if (bidHistoryArea != null) {
      bidHistoryArea.setText("Đang tải lịch sử đặt giá...");
    }
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
    auctionProxy = null;
    preview = null;
    auction = null;
    detailRequestInFlight = false;
    requestedAuctionId = null;
    setDetailLoading(false);
    lastKnownStatus = null;
    imageFetchInFlight.clear();
  }

  /** Member. */
  @FXML
  public void switchToUi(ActionEvent event) {
    cleanup();
    LiveAuctionSessionStore.getInstance().clear();
    NavigationManager.getInstance().navigateTo(View.UI);
  }

  private void handleItemImage(Item item) {
    if (item == null) return;
    handleItemImage(item.getId(), item.getImageUrl());
  }

  private void handleItemImage(int itemId, String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      clearItemImage();
      return;
    }

    // Kiểm tra cache local trước — tránh fetch lại mỗi lần nhận update
    ItemStore.getInstance()
        .getItemImageBase64(itemId)
        .ifPresentOrElse(
            base64 -> displayBase64Image(itemId, base64),
            () -> {
              // Chưa có trong cache, gửi request nếu chưa có request đang bay
              if (imageFetchInFlight.add(itemId)) { // add() trả về false nếu đã tồn tại
                try {
                  requests.fetchItemImage(itemId, imageUrl);
                  logger.debug("Sent FETCH_ITEM_IMAGE for itemId={}", itemId);
                } catch (java.io.IOException e) {
                  imageFetchInFlight.remove(itemId);
                  logger.warn("Cannot send FETCH_ITEM_IMAGE: {}", e.getMessage());
                }
              }
            });
  }

  /** Decode Base64 → JavaFX Image → set lên ImageView. PHẢI chạy trên JavaFX Application Thread. */
  private void displayBase64Image(int itemId, String base64Data) {
    imageFetchInFlight.remove(itemId); // request đã hoàn thành, xóa khỏi in-flight
    try {
      byte[] imageBytes = Base64.getDecoder().decode(base64Data);
      Image image = new Image(new ByteArrayInputStream(imageBytes));
      if (image.isError() || itemImageView == null) return;

      // Đang trên FX thread — set trực tiếp, không cần Platform.runLater()
      itemImageView.setImage(image);
      itemImageView.setVisible(true);
      itemImageView.setManaged(true);
      if (imagePlaceholderLabel != null) {
        imagePlaceholderLabel.setVisible(false);
        imagePlaceholderLabel.setManaged(false);
      }
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid Base64 image data for itemId={}: {}", itemId, e.getMessage());
    }
  }

  private void clearItemImage() {
    if (itemImageView == null) return;
    itemImageView.setImage(null);
    itemImageView.setVisible(false);
    itemImageView.setManaged(false);
    if (imagePlaceholderLabel != null) {
      imagePlaceholderLabel.setVisible(true);
      imagePlaceholderLabel.setManaged(true);
    }
  }
}
