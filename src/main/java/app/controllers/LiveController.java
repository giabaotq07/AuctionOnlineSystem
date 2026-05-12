package app.controllers;

import app.controllers.manager.NavigationManager;
import app.data.AuctionDetail;
import app.data.AuctionDetailRequest;
import app.data.AuctionDetailResponse;
import app.data.AuctionResultRequest;
import app.data.AuctionResultResponse;
import app.data.PlaceBidRequest;
import app.data.PlaceBidResponse;
import app.data.SettleWalletRequest;
import app.data.WalletUpdateResponse;
import app.enums.AuctionStatus;
import app.enums.OperationStatus;
import app.enums.PacketType;
import app.enums.View;
import app.models.Auction;
import app.models.DataStore;
import app.models.PacketReq;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import app.network.Client;
import app.network.PacketListener;
import app.observer.AuctionObserver;
import app.utils.AlertUtils;
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

public class LiveController implements AuctionObserver, Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(LiveController.class);
  private Auction session;
  private long currentPrice;
  private AuctionDetail auctionDetail;
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
  @FXML private Label availableBalanceLabel;
  private ScheduledExecutorService scheduler;
  private boolean resultRequested = false;
  private boolean auctionClosedShown = false;
  private boolean cleanedUp = false;
  private PacketListener<WalletUpdateResponse> walletUpdateHandler;
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
  private boolean settlementSent = false;

  @FXML
  public void initialize() {
    placeBidHandler = response -> Platform.runLater(() -> handleBidResponse(response));
    Client.getInstance().subscribe(PacketType.PLACE_BID, placeBidHandler);
    auctionDetailHandler = response -> Platform.runLater(() -> handleDetailResponse(response));
    Client.getInstance().subscribe(PacketType.FETCH_AUCTION_DETAIL, auctionDetailHandler);
    auctionResultHandler = response -> Platform.runLater(() -> handleAuctionResult(response));
    Client.getInstance().subscribe(PacketType.FETCH_AUCTION_RESULT, auctionResultHandler);
    walletUpdateHandler = response -> Platform.runLater(() -> handleWalletUpdate(response));
    Client.getInstance().subscribe(PacketType.WALLET_UPDATE, walletUpdateHandler);
    updateAvailableBalance();
  }

  public void setSession(Auction session) {
    this.session = session;
    session.registerObserver(this);
    try {
      AuctionDetailRequest request = new AuctionDetailRequest(session.getId());
      Client.getInstance().sendRequest(PacketReq.of(PacketType.FETCH_AUCTION_DETAIL, request));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void handleDetailResponse(AuctionDetailResponse response) {
    if (session == null) {
      return;
    }
    if (!response.success()
        || response.detail() == null
        || response.detail().auctionId() != session.getId()) {
      return;
    }
    auctionDetail = response.detail();
    applyDetail(auctionDetail);
  }

  private void handleBidResponse(PlaceBidResponse response) {
    if (session == null || response.auctionId() != session.getId()) {
      return;
    }
    currentPrice = Math.max(currentPrice, response.highestBidAmount());
    currentPriceLabel.setText(formatCurrency(currentPrice));
    bidAmountField.clear();
    if (Client.getInstance().getCurrentUser() != null
        && response.bidderId() == Client.getInstance().getCurrentUser().getId()) {
      AlertUtils.showInfo("Thành công", "Đặt giá thành công!");
    }
  }

  private void handleWalletUpdate(WalletUpdateResponse response) {
    if (response == null) {
      return;
    }
    if (response.status() != OperationStatus.SUCCESS) {
      AlertUtils.showError("Ví", response.message());
      return;
    }
    if (response.user() != null) {
      DataStore.getInstance().updateCurrentUser(response.user());
      updateAvailableBalance(UserFactory.createUser(response.user()));
    } else {
      updateAvailableBalance();
    }
  }

  private void updateAvailableBalance() {
    updateAvailableBalance(Client.getInstance().getCurrentUser());
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
    if (session == null) {
      return;
    }
    if (!response.success() || response.auctionId() != session.getId()) {
      return;
    }
    onAuctionClosed(
        auctionDetail != null ? auctionDetail.itemName() : "",
        response.winner().name(),
        response.finalPrice());
    requestWalletSettlement();
  }

  private void requestWalletSettlement() {
    if (settlementSent || session == null) {
      return;
    }
    if (Client.getInstance().getCurrentUser() == null) {
      return;
    }
    try {
      settlementSent = true;
      SettleWalletRequest request = new SettleWalletRequest(session.getId());
      Client.getInstance().sendRequest(PacketReq.of(PacketType.SETTLE_WALLET, request));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void applyDetail(AuctionDetail detail) {
    itemNameLabel.setText(detail.itemName());
    startPriceLabel.setText(formatCurrency(detail.startingPrice()));
    stepPriceLabel.setText(formatCurrency(detail.stepPrice()));
    currentPrice = Math.max(currentPrice, detail.currentPrice());
    currentPriceLabel.setText(formatCurrency(currentPrice));
    depositLabel.setText(formatCurrency((long) (detail.startingPrice() * 0.2)));
    description.setText(detail.description());
    startCountdownTimer(detail.endTime());
  }

  @Override
  public void onNewBidPlaced(String itemName, long newPrice, String bidderName) {
    Platform.runLater(
        () -> {
          currentPrice = Math.max(currentPrice, newPrice);
          currentPriceLabel.setText(formatCurrency(currentPrice));
        });
  }

  @Override
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

  @FXML
  public void handlePlaceBid(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }
    if (session == null) {
      AlertUtils.showError("Lỗi", "Phiên không trong thời gian đặt giá");
      return;
    }
    if (Client.getInstance().getCurrentUser() == null) {
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
    BigDecimal available = Client.getInstance().getCurrentUser().getWallet().getAvailableBalance();
    if (available != null && available.compareTo(BigDecimal.valueOf(bidAmount)) < 0) {
      AlertUtils.showError("Lỗi", "Số dư khả dụng không đủ để đặt giá");
      return;
    }
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
                  if (!resultRequested) {
                    resultRequested = true;
                    requestAuctionResult();
                  }
                  scheduler.shutdownNow();
                  if (session != null && session.isRunning()) {
                    session.setStatus(AuctionStatus.FINISHED);
                  }
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
    if (session != null) {
      session.removeObserver(this);
    }
    if (placeBidHandler != null) {
      Client.getInstance().unsubscribe(PacketType.PLACE_BID, placeBidHandler);
    }
    if (auctionDetailHandler != null) {
      Client.getInstance().unsubscribe(PacketType.FETCH_AUCTION_DETAIL, auctionDetailHandler);
    }
    if (auctionResultHandler != null) {
      Client.getInstance().unsubscribe(PacketType.FETCH_AUCTION_RESULT, auctionResultHandler);
    }
    if (walletUpdateHandler != null) {
      Client.getInstance().unsubscribe(PacketType.WALLET_UPDATE, walletUpdateHandler);
    }
    resultRequested = false;
    auctionClosedShown = false;
    settlementSent = false;
    auctionDetail = null;
    session = null;
  }

  @FXML
  public void SwitchToUI(ActionEvent event) {
    cleanup();
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
