package app.client.controllers;

import static app.common.enums.AuctionStatus.*;

import app.client.manager.AuctionDetailProxy;
import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.store.ItemStore;
import app.client.store.LiveAuctionSessionStore;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** LiveController. */
public class LiveController implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(LiveController.class);
  private AuctionDetailProxy auctionProxy;
  private AuctionPreview preview;
  private Auction auction;
  private long currentPrice;

  // === FXML Bindings ===
  @FXML private Label titerTimer;
  @FXML private Label timeLabel;
  @FXML private Label itemNameLabel;
  @FXML private Label descriptionLabel;
  @FXML private Label sellerNameLabel;
  @FXML private Label statusBadge;
  @FXML private Label startPriceLabel;
  @FXML private Label stepPriceLabel;
  @FXML private Label currentPriceLabel;
  @FXML private Label bidCountLabel;
  @FXML private Label statusLabel;
  @FXML private Label bidHintLabel;
  @FXML private Label availableBalanceLabel;
  @FXML private Label leaderNameLabel;
  @FXML private Label leaderPriceLabel;
  @FXML private Circle leaderAvatar;
  @FXML private TextField bidAmountField;
  @FXML private ScrollPane bidHistoryScrollPane;
  @FXML private VBox bidHistoryList;
  @FXML private ProgressIndicator detailLoadingIndicator;
  @FXML private ImageView itemImageView;
  @FXML private Label imagePlaceholderLabel;
  @FXML private StackPane priceChartContainer;
  @FXML private Canvas priceChartCanvas;
  @FXML private TextField autoBidMaxField;
  @FXML private TextField autoBidStepField;
  @FXML private Label autoBidStatusLabel;
  @FXML private Button setAutoBidBtn;
  @FXML private Button disableAutoBidBtn;

  private final Set<Integer> imageFetchInFlight = ConcurrentHashMap.newKeySet();
  private ScheduledExecutorService scheduler;
  private boolean resultRequested = false;
  private boolean auctionClosedShown = false;
  private boolean cleanedUp = false;
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
  private final DateTimeFormatter bidTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
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

  /** Dữ liệu lịch sử giá để vẽ chart. */
  private final List<ChartPoint> priceHistory = new ArrayList<>();

  /** Member. */
  @FXML
  public void initialize() {
    notifications.addUpdateListener(updateListener);
    notifications.addMessageListener(messageListener);
    updateAvailableBalance();
    configureBidHistoryScroll();
    configurePriceChart();
    loadSessionAuction();
    maybeRequestAuctionDetail();
    updateAutoBidUi();
  }

  private void configurePriceChart() {
    if (priceChartContainer == null || priceChartCanvas == null) {
      return;
    }
    priceChartCanvas.setManaged(false);
    priceChartContainer
        .widthProperty()
        .addListener((obs, oldValue, newValue) -> resizePriceChart());
    priceChartContainer
        .heightProperty()
        .addListener((obs, oldValue, newValue) -> resizePriceChart());
    Platform.runLater(this::resizePriceChart);
  }

  private void resizePriceChart() {
    if (priceChartContainer == null || priceChartCanvas == null) {
      return;
    }
    double width = priceChartContainer.getWidth();
    double height = priceChartContainer.getHeight();
    if (width <= 1 || height <= 1) {
      return;
    }
    boolean changed = false;
    if (Math.abs(priceChartCanvas.getWidth() - width) > 0.5) {
      priceChartCanvas.setWidth(width);
      changed = true;
    }
    if (Math.abs(priceChartCanvas.getHeight() - height) > 0.5) {
      priceChartCanvas.setHeight(height);
      changed = true;
    }
    if (changed) {
      drawPriceChart();
    }
  }

  private void configureBidHistoryScroll() {
    if (bidHistoryScrollPane == null) {
      return;
    }
    bidHistoryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    bidHistoryScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    bidHistoryScrollPane.addEventFilter(ScrollEvent.SCROLL, this::scrollBidHistory);
  }

  private void scrollBidHistory(ScrollEvent event) {
    if (bidHistoryScrollPane == null || bidHistoryList == null) {
      return;
    }
    double contentHeight = bidHistoryList.getBoundsInLocal().getHeight();
    double viewHeight = bidHistoryScrollPane.getViewportBounds().getHeight();
    double maxScroll = contentHeight - viewHeight;
    if (maxScroll <= 0) {
      return;
    }
    double nextPixel = bidHistoryScrollPane.getVvalue() * maxScroll - event.getDeltaY();
    bidHistoryScrollPane.setVvalue(clamp(nextPixel / maxScroll));
    event.consume();
  }

  private double clamp(double value) {
    return Math.max(0, Math.min(1, value));
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
      availableBalanceLabel.setText("Balance: $0");
      return;
    }
    Wallet wallet = user.getWallet();
    if (wallet == null) {
      availableBalanceLabel.setText("Available: $0");
      return;
    }
    BigDecimal available = wallet.getAvailableBalance();
    availableBalanceLabel.setText("Available: $" + formatCurrency(available));
  }

  private String formatCurrency(BigDecimal amount) {
    if (amount == null) {
      return "0";
    }
    return currencyFormat.format(amount);
  }

  private String formatCurrency(long amount) {
    return String.format("$%,d", amount);
  }

  /** Format cho hiển thị giá dạng $. */
  private String formatDollar(long amount) {
    return "$" + currencyFormat.format(amount);
  }

  private void handleUpdateNotification() {
    loadSessionAuction();
    refreshDetailFromStore();
    maybeRequestAuctionDetail();
    updateAvailableBalance();
    updateTimer();
    updateAutoBidUi();
  }

  private void updateTimer() {
    if (selectedStatus() == OPEN) {
      titerTimer.setText("BẮT ĐẦU SAU");
    }
    if (selectedStatus() == RUNNING) {
      titerTimer.setText("THỜI GIAN CÒN LẠI");
    }
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
    if (message.contains("đã bị vượt") || message.contains("Auto-bid")) {
      AlertUtils.showInfo("Cảnh báo Auto Bid", message);
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
      currentPriceLabel.setText(formatDollar(currentPrice));
    }
  }

  private void applyPreview(AuctionPreview preview) {
    itemNameLabel.setText(
        preview.itemName() == null ? "(Không có tên tài sản)" : preview.itemName());
    if (descriptionLabel != null) {
      descriptionLabel.setText("Đang tải mô tả chi tiết...");
    }
    startPriceLabel.setText(formatDollar(preview.startingPrice()));
    stepPriceLabel.setText(formatDollar(Math.max(preview.stepPrice(), 1L)));
    currentPrice = Math.max(currentPrice, preview.highestBid());
    currentPriceLabel.setText(formatDollar(currentPrice));
    updateBidCount(0);
    updateBidHint();
    showAwaitingBidHistory();
    updateStatusBadge(preview.status());
    updateSellerName(preview);
    if (preview.status() == OPEN && preview.startTime() != null) {
      startCountdownTimer(preview.startTime(), false);
      titerTimer.setText("BẮT ĐẦU SAU");
    } else if (preview.status() == RUNNING && preview.endTime() != null) {
      startCountdownTimer(preview.endTime());
      titerTimer.setText("THỜI GIAN CÒN LẠI");
    } else {
      updateTimer();
    }
    handleItemImage(preview.itemId(), preview.imageUrl());

    // Cập nhật giá vào chart
    addPricePoint(currentPrice);
    drawPriceChart();
    updateAutoBidUi();
  }

  private void applyDetail(Auction detail) {
    Item item = detail.getItem();
    long startingPrice = item == null ? detail.getHighestBid() : item.getStartingPrice();
    long stepPrice = item == null ? 1L : item.getStepPrice();
    itemNameLabel.setText(item == null ? "(Không có tên tài sản)" : item.getName());
    if (descriptionLabel != null) {
      descriptionLabel.setText(item == null ? "" : item.getDescription());
    }
    startPriceLabel.setText(formatDollar(startingPrice));
    stepPriceLabel.setText(formatDollar(stepPrice));
    currentPrice = Math.max(currentPrice, detail.getHighestBid());
    currentPriceLabel.setText(formatDollar(currentPrice));
    updateBidHistory(detail);
    updateStatusBadge(detail.getStatus());
    updateSellerInfo(detail);
    updateLeaderInfo(detail);
    if (detail.getStatus() == OPEN && detail.getStartTime() != null) {
      startCountdownTimer(detail.getStartTime(), false);
      titerTimer.setText("BẮT ĐẦU SAU");
    } else if (detail.getStatus() == RUNNING && detail.getEndTime() != null) {
      startCountdownTimer(detail.getEndTime());
      titerTimer.setText("THỜI GIAN CÒN LẠI");
    } else {
      updateTimer();
    }
    handleItemImage(item);

    // Cập nhật chart từ bid history
    rebuildChartFromBids(detail);
    drawPriceChart();
    updateAutoBidUi();
  }

  // === BID HISTORY - Rich UI rows thay vì TextArea ===

  private void updateBidHistory(Auction detail) {
    if (bidHistoryList == null) {
      return;
    }
    if (detail == null || detail.getBids().isEmpty()) {
      bidHistoryList.getChildren().clear();
      Label empty = new Label("Chưa có lượt đặt giá.");
      empty.getStyleClass().add("live-bid-time");
      bidHistoryList.getChildren().add(empty);
      updateBidCount(0);
      updateLeaderEmpty();
      return;
    }
    List<Bid> bids = detail.getBids();
    updateBidCount(bids.size());
    updateBidHint();

    bidHistoryList.getChildren().clear();
    boolean isFirst = true;
    for (Bid bid : bids) {
      if (bid == null) {
        continue;
      }
      bidHistoryList.getChildren().add(createBidRow(bid, isFirst));
      isFirst = false;
    }
  }

  private void setDefaultAvatarStyle(Circle circle, String name) {
    if (circle == null) {
      return;
    }
    int hash = Math.abs(name.hashCode());
    Color[] colors = {
      Color.web("#4f46e5"), Color.web("#0891b2"), Color.web("#059669"),
      Color.web("#d97706"), Color.web("#dc2626"), Color.web("#7c3aed")
    };
    circle.setFill(colors[hash % colors.length]);
    circle.setStroke(colors[hash % colors.length].deriveColor(0, 1, 1.3, 1));
    circle.setStrokeWidth(1.5);
  }

  /** Tạo 1 row trong bid history list giống mockup. */
  private HBox createBidRow(Bid bid, boolean isLeader) {
    HBox row = new HBox(10);
    row.setAlignment(Pos.CENTER_LEFT);
    row.getStyleClass().add("live-bid-row");

    // Avatar circle
    Circle avatar = new Circle(16);
    avatar.getStyleClass().add("live-bid-avatar");
    String bidderName =
        bid.getBidderName() == null ? "Bidder #" + bid.getBidderId() : bid.getBidderName();

    User bidder = bid.getBidder();
    String avatarUrl = (bidder != null) ? bidder.getAvatarUrl() : null;
    if (avatarUrl != null && !avatarUrl.isBlank()) {
      java.util.Optional<String> base64Opt =
          UserManager.getInstance().getAvatarBase64(bid.getBidderId());
      if (base64Opt.isPresent()) {
        try {
          byte[] bytes = java.util.Base64.getDecoder().decode(base64Opt.get());
          Image image = new Image(new java.io.ByteArrayInputStream(bytes));
          avatar.setFill(new ImagePattern(image));
          avatar.setStroke(Color.web("#e2e8f0"));
          avatar.setStrokeWidth(1.5);
        } catch (Exception e) {
          setDefaultAvatarStyle(avatar, bidderName);
        }
      } else {
        setDefaultAvatarStyle(avatar, bidderName);
        try {
          requests.fetchAvatar(bid.getBidderId(), avatarUrl);
        } catch (IOException e) {
          logger.error("Failed to request bidder avatar", e);
        }
      }
    } else {
      setDefaultAvatarStyle(avatar, bidderName);
    }

    // Name
    Label nameLabel = new Label(bidderName);
    nameLabel.getStyleClass().add("live-bid-name");
    nameLabel.setMinWidth(80);

    // Amount
    Label amountLabel = new Label(formatDollar(bid.getAmount()));
    amountLabel.getStyleClass().add("live-bid-amount");

    // Time
    String timeText = bid.getCreateAt() == null ? "" : bidTimeFormat.format(bid.getCreateAt());
    Label timeLabel = new Label(timeText);
    timeLabel.getStyleClass().add("live-bid-time");

    // Spacer to push time/badge to right
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox rightSide = new HBox(8);
    rightSide.setAlignment(Pos.CENTER_RIGHT);
    rightSide.getChildren().addAll(timeLabel);

    if (isLeader) {
      Label badge = new Label("Mới nhất");
      badge.getStyleClass().add("live-bid-first-badge");
      rightSide.getChildren().add(badge);
    }

    row.getChildren().addAll(avatar, nameLabel, amountLabel, spacer, rightSide);
    return row;
  }

  // === LEADER INFO ===

  private void updateLeaderInfo(Auction detail) {
    if (leaderNameLabel == null || leaderPriceLabel == null) {
      return;
    }
    List<Bid> bids = detail.getBids();
    if (bids.isEmpty()) {
      updateLeaderEmpty();
      return;
    }
    Bid topBid = bids.get(0); // Bid đầu tiên là bid cao nhất (sorted from server)
    String name =
        topBid.getBidderName() == null ? "Bidder #" + topBid.getBidderId() : topBid.getBidderName();
    leaderNameLabel.setText(name);
    leaderPriceLabel.setText(formatDollar(topBid.getAmount()));

    // Đổi màu avatar theo người dẫn đầu
    if (leaderAvatar != null) {
      User bidder = topBid.getBidder();
      String avatarUrl = (bidder != null) ? bidder.getAvatarUrl() : null;
      if (avatarUrl != null && !avatarUrl.isBlank()) {
        java.util.Optional<String> base64Opt =
            UserManager.getInstance().getAvatarBase64(topBid.getBidderId());
        if (base64Opt.isPresent()) {
          try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64Opt.get());
            Image image = new Image(new java.io.ByteArrayInputStream(bytes));
            leaderAvatar.setFill(new ImagePattern(image));
            leaderAvatar.setStroke(Color.web("#e2e8f0"));
            leaderAvatar.setStrokeWidth(1.5);
          } catch (Exception e) {
            setDefaultAvatarStyle(leaderAvatar, name);
          }
        } else {
          setDefaultAvatarStyle(leaderAvatar, name);
          try {
            requests.fetchAvatar(topBid.getBidderId(), avatarUrl);
          } catch (IOException e) {
            logger.error("Failed to request leader avatar", e);
          }
        }
      } else {
        setDefaultAvatarStyle(leaderAvatar, name);
      }
    }
  }

  private void updateLeaderEmpty() {
    if (leaderNameLabel != null) {
      leaderNameLabel.setText("---");
    }
    if (leaderPriceLabel != null) {
      leaderPriceLabel.setText("$0");
    }
  }

  // === SELLER INFO ===

  private void updateSellerInfo(Auction detail) {
    if (sellerNameLabel == null) {
      return;
    }
    User seller = detail.getSeller();
    if (seller != null) {
      sellerNameLabel.setText(seller.getName());
    } else {
      sellerNameLabel.setText("Người bán #" + detail.getSellerId());
    }
  }

  private void updateSellerName(AuctionPreview preview) {
    if (sellerNameLabel == null) {
      return;
    }
    if (preview.seller() != null) {
      sellerNameLabel.setText(preview.seller().name());
    } else {
      sellerNameLabel.setText("Đang tải...");
    }
  }

  // === STATUS BADGE ===

  private void updateStatusBadge(AuctionStatus status) {
    handleStatusTransition(status);
    if (statusBadge == null || status == null) {
      return;
    }
    if (status == AuctionStatus.OPEN) {
      statusBadge.setText("SẮP DIỄN RA");
      statusBadge.setStyle(
          "-fx-background-color: rgba(59,130,246,0.15);"
              + "-fx-text-fill: #3b82f6;"
              + "-fx-border-color: rgba(59,130,246,0.3);");
      return;
    }
    if (status == AuctionStatus.RUNNING) {
      statusBadge.setText("ĐANG DIỄN RA");
      statusBadge.setStyle(
          "-fx-background-color: rgba(34,197,94,0.15);"
              + "-fx-text-fill: #22c55e;"
              + "-fx-border-color: rgba(34,197,94,0.3);");
      return;
    }
    if (status == AuctionStatus.CANCELED) {
      statusBadge.setText("ĐÃ BỊ HỦY");
      statusBadge.setStyle(
          "-fx-background-color: rgba(59,130,246,0.15);"
              + "-fx-text-fill: #3b82f6;"
              + "-fx-border-color: rgba(59,130,246,0.3);");
      return;
    }
    statusBadge.setText("ĐÃ KẾT THÚC");
    statusBadge.setStyle(
        "-fx-background-color: rgba(239,68,68,0.15);"
            + "-fx-text-fill: #ef4444;"
            + "-fx-border-color: rgba(239,68,68,0.3);");
  }

  // === BID COUNT & HINT ===

  private void updateBidCount(int count) {
    if (bidCountLabel != null) {
      bidCountLabel.setText(String.valueOf(count));
    }
  }

  private void updateBidHint() {
    if (bidHintLabel == null) {
      return;
    }
    long min = minimumBid();
    bidHintLabel.setText("Nhập " + formatDollar(min) + " trở lên để vượt giá hiện tại");
  }

  // === QUICK BID ===

  /** Handler cho các nút +$50, +$100, +$500, +$1000. */
  @FXML
  public void handleQuickBid(ActionEvent event) {
    if (!(event.getSource() instanceof Button btn)) {
      return;
    }
    String userData = (String) btn.getUserData();
    if (userData == null) {
      return;
    }
    try {
      long increment = Long.parseLong(userData);
      long current = currentPrice;
      // Nếu đã có giá trị trong ô nhập, cộng thêm vào đó
      String existing = bidAmountField.getText().trim();
      if (!existing.isEmpty()) {
        try {
          current = Long.parseLong(existing);
        } catch (NumberFormatException ignored) {
          // fallback to currentPrice
        }
      } else {
        current = currentPrice;
      }
      bidAmountField.setText(String.valueOf(current + increment));
    } catch (NumberFormatException ignored) {
      // ignore
    }
  }

  // === PRICE CHART (Canvas-based) ===

  /** Điểm dữ liệu cho biểu đồ. */
  private static class ChartPoint {
    final LocalDateTime time;
    final long price;

    ChartPoint(LocalDateTime time, long price) {
      this.time = time;
      this.price = price;
    }
  }

  private void addPricePoint(long price) {
    priceHistory.add(new ChartPoint(LocalDateTime.now(), price));
    priceHistory.sort((left, right) -> left.time.compareTo(right.time));
  }

  /** Rebuild chart data từ bid list. */
  private void rebuildChartFromBids(Auction detail) {
    priceHistory.clear();
    if (detail == null) {
      return;
    }
    Item item = detail.getItem();
    long startingPrice = item == null ? 0 : item.getStartingPrice();
    // Thêm giá khởi điểm
    LocalDateTime startTime = detail.getStartTime();
    if (startTime == null) {
      startTime = LocalDateTime.now().minusMinutes(30);
    }
    if (startingPrice > 0) {
      priceHistory.add(new ChartPoint(startTime, startingPrice));
    }
    List<Bid> bids = new ArrayList<>(detail.getBids());
    bids.removeIf(bid -> bid == null || bid.getCreateAt() == null);
    bids.sort((left, right) -> left.getCreateAt().compareTo(right.getCreateAt()));
    for (Bid bid : bids) {
      priceHistory.add(new ChartPoint(bid.getCreateAt(), bid.getAmount()));
    }
    priceHistory.sort((left, right) -> left.time.compareTo(right.time));
  }

  /** Vẽ biểu đồ giá trực tiếp lên Canvas. */
  private void drawPriceChart() {
    if (priceChartCanvas == null) {
      return;
    }
    GraphicsContext gc = priceChartCanvas.getGraphicsContext2D();
    double w = priceChartCanvas.getWidth();
    double h = priceChartCanvas.getHeight();

    // Clear canvas
    gc.clearRect(0, 0, w, h);

    if (w <= 0 || h <= 0) {
      return;
    }

    double padLeft = 64;
    double padRight = 24;
    double padTop = 56;
    double padBottom = 34;
    double chartW = w - padLeft - padRight;
    double chartH = h - padTop - padBottom;
    if (chartW <= 0 || chartH <= 0) {
      return;
    }

    // Vẽ nền grid
    gc.setStroke(Color.web("#1e293b"));
    gc.setLineWidth(0.5);
    for (int i = 0; i <= 4; i++) {
      double y = padTop + (chartH / 4.0) * i;
      gc.strokeLine(padLeft, y, w - padRight, y);
    }
    gc.setStroke(Color.web("#334155"));
    gc.setLineWidth(1);
    gc.strokeLine(padLeft, padTop, padLeft, padTop + chartH);
    gc.strokeLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH);

    if (priceHistory.isEmpty()) {
      gc.setFill(Color.web("#475569"));
      gc.fillText("Chưa có dữ liệu", w / 2 - 40, h / 2);
      return;
    }

    // Tính min/max
    long minPrice = Long.MAX_VALUE;
    long maxPrice = Long.MIN_VALUE;
    for (ChartPoint p : priceHistory) {
      minPrice = Math.min(minPrice, p.price);
      maxPrice = Math.max(maxPrice, p.price);
    }
    if (minPrice == maxPrice) {
      minPrice = Math.max(0, maxPrice - 1000);
      maxPrice = maxPrice + 1000;
    }
    // Thêm 10% margin
    long range = maxPrice - minPrice;
    minPrice = Math.max(0, minPrice - range / 10);
    maxPrice = maxPrice + range / 10;
    LocalDateTime minTime = priceHistory.get(0).time;
    LocalDateTime maxTime = priceHistory.get(priceHistory.size() - 1).time;
    long rawTimeRangeMillis = ChronoUnit.MILLIS.between(minTime, maxTime);
    long timeRangeMillis = Math.max(1, rawTimeRangeMillis);
    boolean useTimeScale = rawTimeRangeMillis > 0;

    // Vẽ trục Y labels
    gc.setFill(Color.web("#64748b"));
    gc.setFont(javafx.scene.text.Font.font("System", 10));
    for (int i = 0; i <= 4; i++) {
      double y = padTop + (chartH / 4.0) * i;
      long value = maxPrice - (long) ((maxPrice - minPrice) * (i / 4.0));
      gc.fillText(currencyFormat.format(value), 5, y + 4);
    }

    // Vẽ trục X labels (time)
    if (priceHistory.size() > 1) {
      DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
      int labelCount = Math.min(priceHistory.size(), 6);
      for (int i = 0; i < labelCount; i++) {
        int idx = (int) ((double) i / (labelCount - 1) * (priceHistory.size() - 1));
        ChartPoint point = priceHistory.get(idx);
        double x =
            xForPoint(
                point,
                idx,
                priceHistory.size(),
                minTime,
                timeRangeMillis,
                useTimeScale,
                padLeft,
                chartW);
        String timeStr = timeFormat.format(point.time);
        gc.setFill(Color.web("#64748b"));
        fillTextClamped(gc, timeStr, x - 15, h - 7, padLeft, w - padRight - 30);
      }
    }

    // Vẽ đường giá (line chart)
    if (priceHistory.size() == 1) {
      double x = padLeft + chartW / 2;
      double y = padTop + chartH / 2;
      gc.setFill(Color.web("#4f46e5"));
      gc.fillOval(x - 4, y - 4, 8, 8);
      drawLastPointLabel(gc, priceHistory.get(0), x, y, w, padRight);
      return;
    }

    // Vẽ area fill (gradient effect)
    gc.setGlobalAlpha(0.15);
    gc.setFill(Color.web("#4f46e5"));
    gc.beginPath();
    for (int i = 0; i < priceHistory.size(); i++) {
      ChartPoint point = priceHistory.get(i);
      double x =
          xForPoint(
              point,
              i,
              priceHistory.size(),
              minTime,
              timeRangeMillis,
              useTimeScale,
              padLeft,
              chartW);
      double y = yForPrice(point.price, minPrice, maxPrice, padTop, chartH);
      if (i == 0) {
        gc.moveTo(x, y);
      } else {
        gc.lineTo(x, y);
      }
    }
    gc.lineTo(padLeft + chartW, padTop + chartH);
    gc.lineTo(padLeft, padTop + chartH);
    gc.closePath();
    gc.fill();
    gc.setGlobalAlpha(1.0);

    // Vẽ đường line
    gc.setStroke(Color.web("#4f46e5"));
    gc.setLineWidth(2.5);
    gc.beginPath();
    for (int i = 0; i < priceHistory.size(); i++) {
      ChartPoint point = priceHistory.get(i);
      double x =
          xForPoint(
              point,
              i,
              priceHistory.size(),
              minTime,
              timeRangeMillis,
              useTimeScale,
              padLeft,
              chartW);
      double y = yForPrice(point.price, minPrice, maxPrice, padTop, chartH);
      if (i == 0) {
        gc.moveTo(x, y);
      } else {
        gc.lineTo(x, y);
      }
    }
    gc.stroke();

    // Vẽ dots tại mỗi data point
    for (int i = 0; i < priceHistory.size(); i++) {
      ChartPoint point = priceHistory.get(i);
      double x =
          xForPoint(
              point,
              i,
              priceHistory.size(),
              minTime,
              timeRangeMillis,
              useTimeScale,
              padLeft,
              chartW);
      double y = yForPrice(point.price, minPrice, maxPrice, padTop, chartH);

      // Outer glow
      gc.setFill(Color.web("#4f46e5", 0.3));
      gc.fillOval(x - 6, y - 6, 12, 12);
      // Inner dot
      gc.setFill(Color.web("#4f46e5"));
      gc.fillOval(x - 3.5, y - 3.5, 7, 7);
    }

    // Tooltip cho điểm cuối (giá mới nhất)
    if (!priceHistory.isEmpty()) {
      ChartPoint last = priceHistory.get(priceHistory.size() - 1);
      double x =
          xForPoint(
              last,
              priceHistory.size() - 1,
              priceHistory.size(),
              minTime,
              timeRangeMillis,
              useTimeScale,
              padLeft,
              chartW);
      double y = yForPrice(last.price, minPrice, maxPrice, padTop, chartH);
      drawLastPointLabel(gc, last, x, y, w, padRight);
    }
  }

  private double xForPoint(
      ChartPoint point,
      int index,
      int pointCount,
      LocalDateTime minTime,
      long timeRangeMillis,
      boolean useTimeScale,
      double padLeft,
      double chartW) {
    if (!useTimeScale) {
      double normalizedIndex = pointCount <= 1 ? 0.5 : (double) index / (pointCount - 1.0);
      return padLeft + chartW * normalizedIndex;
    }
    long elapsedMillis = Math.max(0, ChronoUnit.MILLIS.between(minTime, point.time));
    double normalizedTime = clamp((double) elapsedMillis / timeRangeMillis);
    return padLeft + chartW * normalizedTime;
  }

  private double yForPrice(long price, long minPrice, long maxPrice, double padTop, double chartH) {
    double normalizedPrice = (double) (price - minPrice) / (maxPrice - minPrice);
    return padTop + chartH * (1.0 - clamp(normalizedPrice));
  }

  private void drawLastPointLabel(
      GraphicsContext gc,
      ChartPoint point,
      double x,
      double y,
      double canvasWidth,
      double padRight) {
    String priceText = formatDollar(point.price);
    String timeText = DateTimeFormatter.ofPattern("HH:mm:ss").format(point.time);
    double boxW = Math.max(112, Math.max(priceText.length(), timeText.length()) * 7.5 + 16);
    double boxH = 36;
    double boxX = Math.max(6, Math.min(x - boxW / 2, canvasWidth - padRight - boxW));
    double boxY = Math.max(6, y - boxH - 12);

    gc.setFill(Color.web("#111827"));
    gc.fillRoundRect(boxX, boxY, boxW, boxH, 7, 7);
    gc.setStroke(Color.web("#475569"));
    gc.setLineWidth(1);
    gc.strokeRoundRect(boxX, boxY, boxW, boxH, 7, 7);
    gc.setStroke(Color.web("#475569"));
    gc.strokeLine(x, boxY + boxH, x, y - 6);

    gc.setFill(Color.web("#e2e8f0"));
    gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 11));
    gc.fillText(timeText, boxX + 8, boxY + 14);
    gc.setFill(Color.web("#22c55e"));
    gc.fillText(priceText, boxX + 8, boxY + 28);
  }

  private void fillTextClamped(
      GraphicsContext gc, String text, double x, double y, double minX, double maxX) {
    gc.fillText(text, Math.max(minX, Math.min(x, maxX)), y);
  }

  // === EXISTING LOGIC (unchanged) ===

  private void showAuctionClosed(String message) {
    if (auctionClosedShown) {
      return;
    }
    auctionClosedShown = true;
    AlertUtils.showInfo("Kết thúc", message);
    timeLabel.setText("Phiên đã kết thúc!");
    timeLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 20px;");
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

  /** handleSetAutoBid. */
  @FXML
  public void handleSetAutoBid(ActionEvent event) {
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }
    int selectedAuctionId = selectedAuctionId();
    if (selectedAuctionId <= 0) {
      AlertUtils.showError("Lỗi", "Phiên không hợp lệ.");
      return;
    }
    User currentUser = UserManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để dùng Auto Bid!");
      return;
    }
    AuctionStatus selectedStatus = selectedStatus();
    if (selectedStatus != RUNNING) {
      AlertUtils.showError("Lỗi", "Chỉ được kích hoạt Auto Bid khi phiên đang diễn ra!");
      return;
    }
    long maxAmount;
    long incrementAmount;
    try {
      maxAmount = Long.parseLong(autoBidMaxField.getText().trim());
    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi", "Giá tối đa không hợp lệ.");
      return;
    }
    try {
      incrementAmount = Long.parseLong(autoBidStepField.getText().trim());
    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi", "Bước tăng không hợp lệ.");
      return;
    }

    // Validate inputs
    long minimumMaxAmount = autoBidMinimumMaxAmount(currentUser);
    if (maxAmount < minimumMaxAmount) {
      AlertUtils.showError(
          "Lỗi", "Giá tối đa phải lớn hơn hoặc bằng: " + formatCurrency(minimumMaxAmount));
      return;
    }
    Item item = auction == null ? null : auction.getItem();
    long stepPrice =
        item == null ? (preview == null ? 1L : preview.stepPrice()) : item.getStepPrice();
    if (incrementAmount < stepPrice) {
      AlertUtils.showError(
          "Lỗi",
          "Bước tăng tự động không được nhỏ hơn bước giá tối thiểu: " + formatCurrency(stepPrice));
      return;
    }

    Wallet wallet = currentUser.getWallet();
    BigDecimal available = wallet == null ? BigDecimal.ZERO : wallet.getAvailableBalance();
    if (available.compareTo(BigDecimal.valueOf(maxAmount)) < 0) {
      AlertUtils.showError("Lỗi", "Số dư khả dụng không đủ để ký quỹ mức tối đa!");
      return;
    }

    try {
      requests.setAutoBid(selectedAuctionId, maxAmount, incrementAmount);
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  /** handleDisableAutoBid. */
  @FXML
  public void handleDisableAutoBid(ActionEvent event) {
    if (!requests.isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }
    int selectedAuctionId = selectedAuctionId();
    if (selectedAuctionId <= 0) {
      AlertUtils.showError("Lỗi", "Phiên không hợp lệ.");
      return;
    }
    try {
      requests.disableAutoBid(selectedAuctionId);
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void updateAutoBidUi() {
    if (autoBidStatusLabel == null) {
      return;
    }
    LiveAuctionSessionStore store = LiveAuctionSessionStore.getInstance();
    if (store.isActiveAutoBidEnabled() && store.getActiveAutoBidMaxAmount() != null) {
      autoBidStatusLabel.setText(
          String.format(
              "Đang chạy (Max: %s, Step: %s)",
              formatDollar(store.getActiveAutoBidMaxAmount()),
              formatDollar(store.getActiveAutoBidIncrementAmount())));
      autoBidStatusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
      if (autoBidMaxField != null && autoBidMaxField.getText().trim().isEmpty()) {
        autoBidMaxField.setText(String.valueOf(store.getActiveAutoBidMaxAmount()));
      }
      if (autoBidStepField != null && autoBidStepField.getText().trim().isEmpty()) {
        autoBidStepField.setText(String.valueOf(store.getActiveAutoBidIncrementAmount()));
      }
    } else {
      autoBidStatusLabel.setText("Chưa thiết lập");
      autoBidStatusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
    }
  }

  private long minimumBid() {
    Item item = auction == null ? null : auction.getItem();
    long previewStepPrice = preview == null ? 1L : Math.max(preview.stepPrice(), 1L);
    long stepPrice = item == null ? previewStepPrice : Math.max(item.getStepPrice(), 1L);
    return currentPrice + stepPrice;
  }

  private long autoBidMinimumMaxAmount(User currentUser) {
    if (currentUser != null
        && auction != null
        && auction.getWinnerId() != null
        && auction.getWinnerId() == currentUser.getId()) {
      return currentPrice;
    }
    return minimumBid();
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
    startCountdownTimer(endTime, true);
  }

  private void startCountdownTimer(LocalDateTime targetTime, boolean requestResult) {
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
                  updateCountdownLabel(now, targetTime);
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
    if (status == AuctionStatus.CANCELED) {
      statusLabel.setText("Đã bị hủy");
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
    long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    timeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    // Reset style để giữ màu cyan đẹp
    timeLabel.setStyle("");
  }

  private void showAwaitingServerConfirmation() {
    timeLabel.setText("Đang chờ xác nhận...");
    timeLabel.setStyle("-fx-text-fill: #c77d00; -fx-font-weight: bold; -fx-font-size: 18px;");
  }

  private void showAwaitingAuctionDetail() {
    if (timeLabel == null) {
      return;
    }
    timeLabel.setText("Đang tải...");
    timeLabel.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 16px;");
  }

  private void showAwaitingBidHistory() {
    if (bidHistoryList != null) {
      bidHistoryList.getChildren().clear();
      Label loading = new Label("Đang tải lịch sử...");
      loading.getStyleClass().add("live-bid-time");
      bidHistoryList.getChildren().add(loading);
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
                  requests.fetchItemImage(itemId);
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
