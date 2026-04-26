package app.controllers;

import app.models.AuctionObserver;
import app.models.AuctionSession;
import app.models.DataStore;
import app.network.Client;
import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
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
import javafx.scene.control.TextField;

public class LiveController implements AuctionObserver {
  private AuctionSession session;

  @FXML private Label itemNameLabel;
  @FXML private Label startPriceLabel;
  @FXML private Label stepPriceLabel;
  @FXML private Label currentPriceLabel;
  @FXML private Label depositLabel;
  @FXML private Label timeLabel;
  @FXML private TextField bidAmountField;

  private ScheduledExecutorService scheduler;

  public void setSession(AuctionSession session) {
    this.session = session;
    if (session != null && session.getItem() != null) {
      if (itemNameLabel != null) itemNameLabel.setText(session.getItem().getName());
      if (startPriceLabel != null) startPriceLabel.setText(String.format("%,.0f đ", session.getItem().getStartingPrice()));
      if (stepPriceLabel != null) stepPriceLabel.setText(String.format("%,.0f đ", session.getItem().getStepPrice()));
      if (currentPriceLabel != null) currentPriceLabel.setText(String.format("%,.0f đ", session.getCurrentHighestPrice()));
      if (depositLabel != null) depositLabel.setText(String.format("%,.0f đ", session.getItem().getStartingPrice() * 0.2));

      session.registerObserver(this);
      startCountdownTimer();
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
      AlertUtils.showInfo("Kết thúc", "Phiên đấu giá đã kết thúc. Người thắng: " + winnerName + " với giá: " + finalPrice);
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
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
      return;
    }
    if (DataStore.currentUser == null) {
      AlertUtils.showError("Lỗi", "Bạn phải đăng nhập để trả giá!");
      return;
    }

    try {
      double bidAmount = Double.parseDouble(bidAmountField.getText());

      if (!session.placeBid(DataStore.currentUser, bidAmount)) {
        AlertUtils.showError("Lỗi trả giá", "Không thể trả giá! Giá nhập phải lớn hơn bằng giá hiện tại + bước giá hoặc phiên đấu giá đã kết thúc.");
      } else {
        bidAmountField.clear();
      }
    } catch (NumberFormatException e) {
      AlertUtils.showError("Lỗi nhập liệu", "Vui lòng nhập một số tiền hợp lệ!");
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
          timeLabel.setText("Phiên đấu giá đã kết thúc!");
          timeLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
          scheduler.shutdown();
          session.setStatus(app.models.AuctionStatus.COMPLETED);
        } else {
          long days = ChronoUnit.DAYS.between(now, endTime);
          LocalDateTime temp = now.plusDays(days);
          long hours = ChronoUnit.HOURS.between(temp, endTime);
          temp = temp.plusHours(hours);
          long minutes = ChronoUnit.MINUTES.between(temp, endTime);
          temp = temp.plusMinutes(minutes);
          long seconds = ChronoUnit.SECONDS.between(temp, endTime);

          String remainingTime = String.format("%d Ngày %02d:%02d:%02d", days, hours, minutes, seconds);
          timeLabel.setText(remainingTime);
        }
      });
    }, 0, 1, TimeUnit.SECONDS);
  }

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
    }
    if (session != null) {
      session.removeObserver(this);
    }
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
