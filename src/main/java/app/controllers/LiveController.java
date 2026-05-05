package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.enums.AuctionStatus;
import app.enums.CommandType;
import app.models.Auction;
import app.models.AuctionObserver;
import app.models.BidTransaction;
import app.models.DataStore;
import app.network.Client;
import app.service.AuctionService;
import app.service.BidService;
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
  private final AuctionService auctionService;
  private final BidService bidService;

  @FXML private Label itemNameLabel;
  @FXML private Label startPriceLabel;
  @FXML private Label stepPriceLabel;
  @FXML private Label currentPriceLabel;
  @FXML private Label depositLabel;
  @FXML private Label timeLabel;
  @FXML private TextField bidAmountField;
  @FXML private TextArea description;

  private ScheduledExecutorService scheduler;

  public LiveController() {
    // Khởi tạo Service bằng cách truyền DAO tương ứng vào
    this.auctionService = new AuctionService(AuctionDAO.getInstance());
    this.bidService = new BidService(BidDAO.getInstance());
  }

  public void setSession(Auction session) {
    this.session = session;
    if (session != null && session.getItem() != null) {
      if (itemNameLabel != null) itemNameLabel.setText(session.getItem().getName());
      if (startPriceLabel != null)
        startPriceLabel.setText(String.format("%,.0f đ", session.getItem().getStartingPrice()));
      if (stepPriceLabel != null)
        stepPriceLabel.setText(String.format("%,.0f đ", session.getItem().getStepPrice()));

      // ✅ Lấy giá cao nhất từ database thay vì in-memory
      if (currentPriceLabel != null) {
        double highestBid = bidService.getHighestBidAmount(session.getId());
        double displayPrice = (highestBid > 0) ? highestBid : session.getItem().getStartingPrice();
        currentPriceLabel.setText(String.format("%,.0f đ", displayPrice));
      }

      if (depositLabel != null)
        depositLabel.setText(String.format("%,.0f đ", session.getItem().getStartingPrice() * 0.2));

      session.registerObserver(this);
      startCountdownTimer();
      if (description != null) {
        description.setText(session.getItem().getDescription());
      }
    }
  }

  @Override
  public void onNewBidPlaced(String itemName, double newPrice, String bidderName) {
    Platform.runLater(() -> {
      if (currentPriceLabel != null) {
        currentPriceLabel.setText(String.format("%,.0f đ", newPrice));
      }
    });
  }

  @Override
  public void onAuctionClosed(String itemName, String winnerName, double finalPrice) {
    Platform.runLater(() -> {
      AlertUtils.showInfo(
              "Kết thúc",
              "Phiên đấu giá đã kết thúc. Người thắng: " + winnerName + " với giá: " + String.format("%,.0f đ", finalPrice));
      timeLabel.setText("Phiên đấu giá đã kết thúc!");
      timeLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
      if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdown();
      }
    });
  }

  @FXML
  public void handlePlaceBid(ActionEvent event) {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }

    if (DataStore.currentUser == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để trả giá!");
      return;
    }

    try {
      double bidAmount = Double.parseDouble(bidAmountField.getText());

      // KIỂM TRA SƠ BỘ: Nếu giá nhập thấp hơn giá đang hiển thị thì chặn luôn ở Client cho nhanh
      double highestBid = bidService.getHighestBidAmount(session.getId());
      double currentHighestPrice = (highestBid > 0) ? highestBid : session.getItem().getStartingPrice();

      if (bidAmount <= currentHighestPrice) {
        AlertUtils.showError("Lỗi trả giá", "Giá đặt phải cao hơn giá hiện tại!");
        return;
      }

      // Gọi BidService để lưu vào MySQL
      try {
        bidService.placeBid(session.getId(), DataStore.currentUser.getId(), bidAmount);

        // ✅ CẬP NHẬT UI NGAY LẬP TỨC từ dữ liệu trong database
        BidTransaction highestBidTransaction = bidService.getHighestBid(session.getId());
        if (highestBidTransaction != null) {
          currentPriceLabel.setText(String.format("%,.0f đ", highestBidTransaction.getAmount()));
        }

        bidAmountField.clear();
        AlertUtils.showInfo("Thành công", "Đặt giá thành công!");

        // Gửi yêu cầu lên Server để đồng bộ với các Client khác
        Client.getInstance().sendRequest(new app.models.MessagePacket<>(CommandType.PLACE_BID, session));

      } catch (Exception e) {
        AlertUtils.showError("Lỗi đặt giá", e.getMessage());
      }

    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi nhập liệu", "Vui lòng nhập số tiền hợp lệ!");
    }
  }

  private void startCountdownTimer() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(() -> {
      Platform.runLater(() -> {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = session.getEndTime();

        if (now.isAfter(endTime)) {
          scheduler.shutdown();

          // Sử dụng AuctionService để cập nhật trạng thái COMPLETED vào MySQL
          auctionService.handleSessionCompletion(session.getId(), bidService);

          // Đồng bộ trạng thái đối tượng trên RAM
          session.setStatus(AuctionStatus.COMPLETED);

          // Lấy thông tin người thắng cuộc từ BidService
          BidTransaction winBid = bidService.getHighestBid(session.getId());
          String winner = (winBid != null) ? winBid.getBidder().getName() : "Không có ai";
          double price = (winBid != null) ? winBid.getAmount() : session.getItem().getStartingPrice();

          // Gửi thông báo đồng bộ tới các Client khác thông qua Server
          Client.getInstance().sendRequest(new app.models.MessagePacket<>(CommandType.PLACE_BID, session));

          onAuctionClosed(session.getItem().getName(), winner, price);
        } else {
          updateCountdownLabel(now, endTime);
        }
      });
    }, 0, 1, TimeUnit.SECONDS);
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
  public void SwitchToUI(ActionEvent event) throws IOException {
    if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdown();
    if (session != null) session.removeObserver(this);
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}