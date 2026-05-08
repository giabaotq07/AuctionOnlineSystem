package app.controllers;

import app.config.NavigationManager;
import app.dao.AuctionDAO;
import app.dao.AutoBidDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.dao.impl.MySqlAuctionDAO;
import app.dao.impl.MySqlAutoBidDAO;
import app.dao.impl.MySqlBidDAO;
import app.dao.impl.MySqlItemDAO;
import app.enums.AuctionStatus;
import app.enums.PacketType;
import app.enums.View;
import app.exception.ServiceException;
import app.models.*;
import app.network.Client;
import app.observer.AuctionObserver;
import app.service.AuctionService;
import app.service.BidObserverService;
import app.service.BidService;
import app.service.ItemService;
import app.utils.AlertUtils;
import app.utils.JsonUtil;
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
  private Item item;
  private final AuctionService auctionService;
  private final BidService bidService;
  private final ItemService itemService;

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
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    ItemDAO itemDAO = new MySqlItemDAO();

    BidObserverService bidObserverService = new BidObserverService();
    this.auctionService = new AuctionService(auctionDAO, bidDAO);
    this.bidService = new BidService(bidDAO, autoBidDAO, auctionDAO, bidObserverService);
    this.itemService = new ItemService(itemDAO);
  }

  public void setSession(Auction session) {
    this.item = itemService.getById(session.getItemId());
    this.session = session;
    if (item != null) {
      if (itemNameLabel != null) itemNameLabel.setText(item.getName());
      if (startPriceLabel != null) startPriceLabel.setText("" + item.getStartingPrice());
      if (stepPriceLabel != null) stepPriceLabel.setText("" + item.getStepPrice());

      if (currentPriceLabel != null) {
        long highestBid = bidService.getHighestBid(session.getId()).orElseThrow().getAmount();
        long displayPrice = (highestBid > 0) ? highestBid : item.getStartingPrice();
        currentPriceLabel.setText("" + displayPrice);
      }

      if (depositLabel != null)
        depositLabel.setText(String.format("%,.0f đ", item.getStartingPrice() * 0.2));

      session.registerObserver(this);
      startCountdownTimer();
      if (description != null) {
        description.setText(item.getDescription());
      }
    }
  }

  @Override
  public void onNewBidPlaced(String itemName, long newPrice, String bidderName) {
    Platform.runLater(
        () -> {
          if (currentPriceLabel != null) {
            currentPriceLabel.setText(String.format("%,.0f đ", newPrice));
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
                  + String.format("%,.0f đ", finalPrice));
          timeLabel.setText("Phiên đấu giá đã kết thúc!");
          timeLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
          if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
          }
        });
  }

  @FXML
  public void handlePlaceBid(ActionEvent event) {
    if (Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server!");
      return;
    }

    if (DataStore.currentUser == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để trả giá!");
      long bidAmount = Long.parseLong(bidAmountField.getText());

      // KIỂM TRA SƠ BỘ: Nếu giá nhập thấp hơn giá đang hiển thị thì chặn luôn ở Client cho nhanh
      long highestBid = bidService.getHighestBid(session.getId()).orElseThrow().getAmount();

      // Gọi BidService để lưu vào MySQL
      try {
        bidService.placeBid(session.getId(), DataStore.currentUser.getId(), bidAmount);

        // ✅ CẬP NHẬT UI NGAY LẬP TỨC từ dữ liệu trong database
        BidTransaction highestBidTransaction =
            bidService.getHighestBid(session.getId()).orElseThrow();
        currentPriceLabel.setText("" + highestBidTransaction.getAmount());

        bidAmountField.clear();
        AlertUtils.showInfo("Thành công", "Đặt giá thành công!");

        // Gửi yêu cầu lên Server để đồng bộ với các Client khác
        Client.getInstance()
            .sendRequest(new Packet(PacketType.PLACE_BID, JsonUtil.toJsonElement(session)));

      } catch (ServiceException e) {
        AlertUtils.showError("Lỗi trả giá", "Giá đặt phải cao hơn giá hiện tại!");
      } catch (Exception e) {
        AlertUtils.showError("Lỗi đặt giá", e.getMessage());
      }
    }
  }

  private void startCountdownTimer() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        () -> {
          Platform.runLater(
              () -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime endTime = session.getEndTime();

                if (now.isAfter(endTime)) {
                  scheduler.shutdown();

                  // Sử dụng AuctionService để cập nhật trạng thái COMPLETED vào MySQL
                  auctionService.handleCompletion(session.getId());

                  // Đồng bộ trạng thái đối tượng trên RAM
                  session.setStatus(AuctionStatus.FINISHED);

                  // Lấy thông tin người thắng cuộc từ BidService
                  BidTransaction winBid = bidService.getHighestBid(session.getId()).orElseThrow();
                  String winner = winBid.getBidderName();
                  long price = winBid.getAmount();

                  // Gửi thông báo đồng bộ tới các Client khác thông qua Server
                  Client.getInstance()
                      .sendRequest(
                          new app.models.Packet(
                              PacketType.PLACE_BID, JsonUtil.toJsonElement(session)));

                  onAuctionClosed(item.getName(), winner, price);
                } else {
                  updateCountdownLabel(now, endTime);
                }
              });
        },
        0,
        1,
        TimeUnit.SECONDS);
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
