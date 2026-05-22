package app.client.controllers;

import app.client.manager.*;
import app.client.store.AuctionStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.AuctionPreview;
import app.common.enums.AuctionStatus;
import app.common.enums.View;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** MyHistoryController. */
public class MyHistoryController implements Cleanable {
  private static final double CARD_WIDTH = 260;
  private static final double CARD_HEIGHT = 320;
  private static final long HISTORY_CACHE_TTL_MS = 45_000;
  @FXML private FlowPane runningPane;
  @FXML private ComboBox<String> typeFilterComboBox;
  @FXML private FlowPane finishedPane;
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private final List<AuctionPreview> auctions = new ArrayList<>();
  private final List<Timeline> countdownTimelines = new ArrayList<>();
  private boolean reloadLoading;
  private Button reloadButton;
  private Runnable stopReloadLoading = () -> {};
  private long lastHistoryFetchAtMs;
  private int historyOwnerUserId = -1;
  private boolean historyLoaded;
  private final Runnable summariesListener =
      () ->
          Platform.runLater(
              () -> {
                loadCachedHistory();
                rebuildUi();
                setReloadLoading(false);
              });

  /** Member. */
  @FXML
  public void initialize() {
    typeFilterComboBox.getItems().addAll("ALL", "ELECTRONICS", "ART", "VEHICLE");
    typeFilterComboBox.setValue("ALL");
    typeFilterComboBox.setOnAction(e -> rebuildUi());
    notifications.addUpdateListener(summariesListener);
    loadCachedHistory();
    rebuildUi();
    requestHistoryIfStale(false);
  }

  private void requestHistoryIfStale(boolean force) {
    var currentUser = UserManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      AuctionStore.getInstance().clearHistory();
      loadCachedHistory();
      rebuildUi();
      historyOwnerUserId = -1;
      lastHistoryFetchAtMs = 0;
      historyLoaded = false;
      return;
    }
    int userId = currentUser.getId();
    long now = System.currentTimeMillis();
    boolean sameOwner = historyOwnerUserId == userId;
    boolean expired = now - lastHistoryFetchAtMs >= HISTORY_CACHE_TTL_MS;
    if (!force && sameOwner && historyLoaded && !expired) {
      return;
    }
    try {
      requests.fetchAuctionHistory(AuctionStore.getInstance().getMaxHistoryVersion());
      historyOwnerUserId = userId;
      lastHistoryFetchAtMs = now;
      historyLoaded = true;
    } catch (Exception e) {
      setReloadLoading(false);
      AlertUtils.showError("Lỗi", e.getMessage());
    }
  }

  private void loadCachedHistory() {
    auctions.clear();
    auctions.addAll(AuctionStore.getInstance().getHistoryAuctionPreviews());
    auctions.sort(
        (left, right) -> {
          if (left == null && right == null) {
            return 0;
          }
          if (left == null) {
            return 1;
          }
          if (right == null) {
            return -1;
          }
          if (left.endTime() == null && right.endTime() == null) {
            return Integer.compare(right.auctionId(), left.auctionId());
          }
          if (left.endTime() == null) {
            return 1;
          }
          if (right.endTime() == null) {
            return -1;
          }
          int timeOrder = right.endTime().compareTo(left.endTime());
          if (timeOrder != 0) {
            return timeOrder;
          }
          return Integer.compare(right.auctionId(), left.auctionId());
        });
  }

  private void rebuildUi() {
    if (runningPane == null || finishedPane == null) {
      return;
    }
    stopCountdownTimelines();
    runningPane.getChildren().clear();
    finishedPane.getChildren().clear();
    for (AuctionPreview auction : auctions) {
      if (auction == null) {
        continue;
      }
      VBox card = createAuctionCard(auction);
      if (isActiveStatus(auction.status())) {
        runningPane.getChildren().add(card);
      } else {
        finishedPane.getChildren().add(card);
      }
    }
  }

  private VBox createAuctionCard(AuctionPreview auction) {
    VBox vbox = new VBox();
    vbox.setPrefWidth(CARD_WIDTH);
    vbox.setMinWidth(CARD_WIDTH);
    vbox.setMaxWidth(CARD_WIDTH);
    vbox.setPrefHeight(CARD_HEIGHT);
    vbox.setStyle(
        "-fx-background-color: #1a1f35;"
            + "-fx-background-radius: 8;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);"
            + "-fx-padding: 15;"
            + "-fx-spacing: 10;");
    StackPane imagePane = new StackPane();
    imagePane.setPrefHeight(100);
    imagePane.setStyle("-fx-background-color: #2a2f45;" + "-fx-background-radius: 5;");
    Label imgLabel = new Label("Ảnh tài sản");
    imgLabel.setStyle("-fx-text-fill: #aaa;");
    imagePane.getChildren().add(imgLabel);
    Label titleLabel = new Label(itemName(auction));
    titleLabel.setWrapText(true);
    titleLabel.setStyle(
        "-fx-font-weight: bold;" + "-fx-font-size: 14px;" + "-fx-text-fill: white;");
    Label priceLabel = new Label("Giá hiện tại: " + auction.highestBid() + " đ");
    priceLabel.setStyle("-fx-text-fill: #e91e63;" + "-fx-font-weight: bold;");
    Label timeLabel = new Label(timeText(auction));
    timeLabel.setStyle("-fx-text-fill: #9aa0b4;" + "-fx-font-size: 12px;");
    if (auction.status() == AuctionStatus.OPEN && auction.startTime() != null) {
      attachStartCountdown(auction.startTime(), timeLabel);
    }
    Button btnDetail =
        new Button(
            auction.status() == AuctionStatus.FINISHED || auction.status() == AuctionStatus.PAID
                ? "Xem kết quả"
                : "Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle(
        "-fx-background-color: #673ab7;" + "-fx-text-fill: white;" + "-fx-cursor: hand;");
    btnDetail.setOnAction(e -> NavigationManager.getInstance().openAuctionDetail(auction));
    vbox.getChildren().addAll(imagePane, titleLabel, priceLabel, timeLabel, btnDetail);
    return vbox;
  }

  private boolean isActiveStatus(AuctionStatus status) {
    return status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING;
  }

  private String timeText(AuctionPreview auction) {
    if (auction.status() == AuctionStatus.OPEN && auction.startTime() != null) {
      return "Bắt đầu sau: " + countdownText(auction.startTime());
    }
    return "Kết thúc: " + (auction.endTime() == null ? "--" : auction.endTime());
  }

  private void attachStartCountdown(LocalDateTime startTime, Label label) {
    Timeline timeline = new Timeline();
    timeline
        .getKeyFrames()
        .add(
            new KeyFrame(
                Duration.seconds(1),
                event -> {
                  if (!LocalDateTime.now().isBefore(startTime)) {
                    label.setText("Đang chờ bắt đầu...");
                    timeline.stop();
                    return;
                  }
                  label.setText("Bắt đầu sau: " + countdownText(startTime));
                }));
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();
    countdownTimelines.add(timeline);
  }

  private String countdownText(LocalDateTime targetTime) {
    long totalSeconds = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), targetTime));
    long days = totalSeconds / 86400;
    long hours = (totalSeconds % 86400) / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    if (days > 0) {
      return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
    }
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  private String itemName(AuctionPreview auction) {
    return auction == null || auction.itemName() == null
        ? "(Không có tên tài sản)"
        : auction.itemName();
  }

  /** Member. */
  @FXML
  public void handleReload(ActionEvent event) {
    if (reloadLoading) {
      return;
    }
    loadCachedHistory();
    rebuildUi();
    try {
      reloadButton = LoadingButton.fromEvent(event);
      setReloadLoading(true);
      requestHistoryIfStale(true);
    } catch (Exception e) {
      setReloadLoading(false);
      AlertUtils.showError("Lỗi", e.getMessage());
    }
  }

  private void setReloadLoading(boolean loading) {
    reloadLoading = loading;
    if (loading) {
      stopReloadLoading = LoadingButton.show(reloadButton);
    } else {
      stopReloadLoading.run();
      stopReloadLoading = () -> {};
    }
  }

  /** Member. */
  @FXML
  public void switchToUi() {
    try {
      NavigationManager.getInstance().navigateTo(View.UI);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void cleanup() {
    notifications.removeUpdateListener(summariesListener);
    setReloadLoading(false);
    stopCountdownTimelines();
  }

  private void stopCountdownTimelines() {
    for (Timeline timeline : countdownTimelines) {
      timeline.stop();
    }
    countdownTimelines.clear();
  }
}
