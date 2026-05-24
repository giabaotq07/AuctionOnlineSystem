package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** AllAuctionController. */
public class AllAuctionController implements Cleanable {
  private static final double CARD_WIDTH = 260;
  private static final double CARD_HEIGHT = 300;
  @FXML private FlowPane runningPane;
  @FXML private ComboBox<String> typeFilterComboBox;
  @FXML private TextField searchField;
  @FXML private FlowPane finishedPane;
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private final AuctionStore store = AuctionStore.getInstance();
  private final List<AuctionPreview> auctions = new ArrayList<>();
  private final List<Timeline> countdownTimelines = new ArrayList<>();
  private boolean reloadLoading;
  private Button reloadButton;
  private Runnable stopReloadLoading = () -> {};
  private final Runnable summariesListener =
      () ->
          Platform.runLater(
              () -> {
                requestAuctions();
                rebuildUi();
                setReloadLoading(false);
              });

  /** Member. */
  @FXML
  public void initialize() {
    typeFilterComboBox.getItems().addAll("ALL", "ELECTRONICS", "ART", "VEHICLE");
    typeFilterComboBox.setValue("ALL");
    typeFilterComboBox.setOnAction(e -> rebuildUi());
    searchField.textProperty().addListener((obs, oldValue, newValue) -> rebuildUi());
    notifications.addUpdateListener(summariesListener);
    requestAuctions();
    rebuildUi();
  }

  private void requestAuctions() {
    auctions.clear();
    auctions.addAll(store.getAuctionPreviews());
  }

  private void rebuildUi() {
    if (runningPane == null || finishedPane == null) {
      return;
    }
    stopCountdownTimelines();
    runningPane.getChildren().clear();
    finishedPane.getChildren().clear();
    String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
    String type = typeFilterComboBox.getValue() == null ? "ALL" : typeFilterComboBox.getValue();
    for (AuctionPreview auction : auctions) {
      if (!matchFilter(auction, query, type)) {
        continue;
      }
      if (isActiveStatus(auction.status())) {
        runningPane.getChildren().add(createAuctionCard(auction));
      } else {
        finishedPane.getChildren().add(createAuctionCard(auction));
      }
    }
  }

  private boolean matchFilter(AuctionPreview auction, String query, String type) {
    if (auction == null) {
      return false;
    }
    String name = itemName(auction);
    if (!query.isBlank() && !name.toLowerCase().contains(query)) {
      return false;
    }
    return "ALL".equals(type)
        || (auction.itemType() != null && auction.itemType().name().equals(type));
  }

  private VBox createAuctionCard(AuctionPreview auction) {
    VBox vbox = new VBox();
    vbox.setPrefWidth(CARD_WIDTH);
    vbox.setMinWidth(CARD_WIDTH);
    vbox.setMaxWidth(CARD_WIDTH);
    vbox.setPrefHeight(CARD_HEIGHT);
    vbox.getStyleClass().add("auction-card");
    StackPane imagePane = new StackPane();
    imagePane.setPrefHeight(100);
    imagePane.getStyleClass().add("auction-image");
    Label imgLabel = new Label("Ảnh tài sản");
    imgLabel.getStyleClass().add("image-placeholder");
    imagePane.getChildren().add(imgLabel);
    Label titleLabel = new Label(itemName(auction));
    titleLabel.setWrapText(true);
    titleLabel.getStyleClass().add("auction-card-title");
    Label priceLabel = new Label("Giá hiện tại: $" + auction.highestBid());
    priceLabel.getStyleClass().add("price-label");
    Label timeLabel = new Label(timeText(auction));
    timeLabel.getStyleClass().add("time-label");
    if (auction.status() == AuctionStatus.OPEN && auction.startTime() != null) {
      attachStartCountdown(auction.startTime(), timeLabel);
    }
    Button btnDetail =
        new Button(
            auction.status() == AuctionStatus.FINISHED || auction.status() == AuctionStatus.PAID
                ? "Xem kết quả"
                : "Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.getStyleClass().add("compact-primary-button");
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
    try {
      reloadButton = LoadingButton.fromEvent(event);
      setReloadLoading(true);
      requests.fetchAuctionSummaries();
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
