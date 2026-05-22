package app.client.controllers;

import app.client.manager.ClientNotificationCenter;
import app.client.manager.ClientRequestService;
import app.client.manager.NavigationManager;
import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.client.store.ItemStore;
import app.client.utils.AlertUtils;
import app.client.utils.LoadingButton;
import app.common.dto.AuctionSummary;
import app.common.enums.AuctionStatus;
import app.common.enums.View;
import app.common.models.User;
import app.common.models.Wallet;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FirstScene. */
public class FirstScene implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(FirstScene.class);
  private static final double CARD_WIDTH = 280;
  private static final double SPACING = 30;
  @FXML private TextField searchField;
  @FXML private ListView<AuctionSummary> auctionListView;
  @FXML private Button btnAuth;
  @FXML private StackPane activeAuctionsPane;
  @FXML private StackPane completedAuctionsPane;
  @FXML private StackPane upcomingAuctionsPane;
  @FXML private Label balanceLabel;
  private final ClientRequestService requests = ClientRequestService.getInstance();
  private final ClientNotificationCenter notifications = ClientNotificationCenter.getInstance();
  private final List<AuctionSummary> summaries = new ArrayList<>();
  private final HBox upcomingBox = new HBox();
  private final HBox activeBox = new HBox();
  private final HBox completedBox = new HBox();
  private final List<Timeline> timelines = new ArrayList<>();
  private final List<Timeline> countdownTimelines = new ArrayList<>();
  private final Set<Integer> imageFetchInFlight = new HashSet<>();
  private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
  private boolean reloadLoading;
  private Button reloadButton;
  private Runnable stopReloadLoading = () -> {};
  private final Runnable summariesListener =
      () ->
          Platform.runLater(
              () -> {
                loadInitialData();
                setReloadLoading(false);
              });

  /** Member. */
  @FXML
  public void initialize() {
    setupHbox(upcomingBox);
    setupHbox(activeBox);
    setupHbox(completedBox);
    setupListView();
    setupAuthButton();
    setupSearch();
    setupScrollPanes();
    setupWalletSection();
    notifications.addUpdateListener(summariesListener);
    loadInitialData();
    logger.debug("FirstScene initialized");
  }

  private void setupListView() {
    auctionListView.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(AuctionSummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                } else {
                  setText(item.itemName());
                }
              }
            });
    auctionListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                NavigationManager.getInstance().openAuctionDetail(newVal);
              }
            });
  }

  private void setupAuthButton() {
    if (btnAuth == null) {
      return;
    }
    if (UserManager.getInstance().getCurrentUser() != null) {
      btnAuth.setText("Thông tin User: " + UserManager.getInstance().getCurrentUser().getName());
    } else {
      btnAuth.setText("Đăng nhập / Đăng ký");
    }
  }

  private void setupSearch() {
    if (searchField == null) {
      return;
    }
    searchField
        .textProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              updateListView();
            });
  }

  private void setupScrollPanes() {
    if (upcomingAuctionsPane != null) {
      upcomingAuctionsPane.getChildren().setAll(createScrollBox(upcomingBox));
    }
    if (activeAuctionsPane != null) {
      activeAuctionsPane.getChildren().setAll(createScrollBox(activeBox));
    }
    if (completedAuctionsPane != null) {
      completedAuctionsPane.getChildren().setAll(createScrollBox(completedBox));
    }
  }

  private void loadInitialData() {
    summaries.clear();
    summaries.addAll(AuctionStore.getInstance().getAuctionSummaries());
    rebuildUi();
  }

  private void rebuildUi() {
    stopCountdownTimelines();
    upcomingBox.getChildren().clear();
    activeBox.getChildren().clear();
    completedBox.getChildren().clear();
    for (AuctionSummary summary : summaries) {
      addAuctionCard(summary);
    }
    updateListView();
  }

  private void addAuctionCard(AuctionSummary summary) {
    VBox card = createAuctionCard(summary);
    if (summary.status() == AuctionStatus.OPEN) {
      upcomingBox.getChildren().add(card);
      return;
    }
    if (summary.status() == AuctionStatus.RUNNING
        || summary.status() == AuctionStatus.ENDING_SOON) {
      activeBox.getChildren().add(card);
      return;
    }
    completedBox.getChildren().add(card);
  }

  private void updateListView() {
    if (auctionListView == null) {
      return;
    }
    auctionListView.getItems().clear();
    String keyword = "";
    if (searchField != null && searchField.getText() != null) {
      keyword = searchField.getText().toLowerCase().trim();
    }
    for (AuctionSummary summary : summaries) {
      String itemName = summary.itemName() == null ? "" : summary.itemName().toLowerCase();
      if (keyword.isBlank() || itemName.contains(keyword)) {
        auctionListView.getItems().add(summary);
      }
    }
  }

  private void setupHbox(HBox hbox) {
    hbox.setAlignment(Pos.CENTER_LEFT);
    hbox.setSpacing(SPACING);
  }

  private ScrollPane createScrollBox(HBox container) {
    ScrollPane viewport = new ScrollPane(container);
    viewport.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    viewport.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    viewport.setFitToHeight(true);
    viewport.setStyle("-fx-background-color: transparent;" + "-fx-background-insets: 0;");
    double viewportWidth = 3 * CARD_WIDTH + 2 * SPACING;
    viewport.setPrefWidth(viewportWidth);
    viewport.setMaxWidth(viewportWidth);
    viewport.setPrefHeight(350);
    Timeline scrollTimeline =
        new Timeline(
            new KeyFrame(
                Duration.seconds(3),
                e -> {
                  double contentWidth = container.getWidth();
                  double viewWidth = viewport.getViewportBounds().getWidth();
                  double maxScroll = contentWidth - viewWidth;
                  if (maxScroll <= 0) {
                    return;
                  }
                  double step = CARD_WIDTH + SPACING;
                  double currentPixel = viewport.getHvalue() * maxScroll;
                  double nextPixel = currentPixel + step;
                  if (nextPixel >= maxScroll) {
                    nextPixel = 0;
                  }
                  viewport.setHvalue(nextPixel / maxScroll);
                }));
    scrollTimeline.setCycleCount(Timeline.INDEFINITE);
    scrollTimeline.play();
    viewport.setOnMouseEntered(e -> scrollTimeline.pause());
    viewport.setOnMouseExited(e -> scrollTimeline.play());
    timelines.add(scrollTimeline);
    return viewport;
  }

  private VBox createAuctionCard(AuctionSummary summary) {
    VBox vbox = new VBox();
    vbox.setPrefWidth(CARD_WIDTH);
    vbox.setMinWidth(CARD_WIDTH);
    vbox.setMaxWidth(CARD_WIDTH);
    vbox.setStyle(
        "-fx-background-color: #1a1f35;"
            + "-fx-background-radius: 8;"
            + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);"
            + "-fx-padding: 15;"
            + "-fx-spacing: 10;");
    StackPane imagePane = new StackPane();
    imagePane.setPrefHeight(150);
    imagePane.setStyle("-fx-background-color: #2a2f45;" + "-fx-background-radius: 5;");
    Label imgLabel = new Label("Ảnh tài sản");
    imgLabel.setStyle("-fx-text-fill: #aaa;");
    ImageView imageView = new ImageView();
    imageView.setFitWidth(CARD_WIDTH - 30);
    imageView.setFitHeight(140);
    imageView.setPreserveRatio(true);
    imageView.setSmooth(true);
    int itemId = summary.itemId();
    String imageUrl = summary.imageUrl();
    Optional<String> cachedBase64 = ItemStore.getInstance().getItemImageBase64(itemId);
    if (cachedBase64.isPresent()) {
      try {
        byte[] imageBytes = java.util.Base64.getDecoder().decode(cachedBase64.get());
        Image image = new Image(new ByteArrayInputStream(imageBytes));
        imageView.setImage(image);
        imgLabel.setVisible(false);
        imgLabel.setManaged(false);
      } catch (Exception e) {
        logger.warn("Failed to decode cached image for item {}", itemId, e);
      }
    } else if (imageUrl != null && !imageUrl.isBlank() && itemId > 0) {
      if (imageFetchInFlight.add(itemId)) {
        try {
          requests.fetchItemImage(itemId, imageUrl);
        } catch (Exception e) {
          logger.warn("Failed to fetch image for item {}", itemId, e);
          imageFetchInFlight.remove(itemId);
        }
      }
    }
    imagePane.getChildren().addAll(imgLabel, imageView);
    Label titleLabel = new Label(summary.itemName());
    titleLabel.setWrapText(true);
    titleLabel.setStyle(
        "-fx-font-weight: bold;" + "-fx-font-size: 14px;" + "-fx-text-fill: white;");
    Label priceLabel = new Label("Giá hiện tại: " + summary.currentPrice() + " đ");
    priceLabel.setStyle("-fx-text-fill: #e91e63;" + "-fx-font-weight: bold;");
    Label timeLabel = new Label(timeText(summary));
    timeLabel.setStyle("-fx-text-fill: #9aa0b4;" + "-fx-font-size: 12px;");
    if (summary.status() == AuctionStatus.OPEN && summary.startTime() != null) {
      attachStartCountdown(summary.startTime(), timeLabel);
    }
    Button btnDetail =
        new Button(
            summary.status() == AuctionStatus.FINISHED || summary.status() == AuctionStatus.PAID
                ? "Xem kết quả"
                : "Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle("-fx-background-color: #673ab7;" + "-fx-text-fill: white;");
    btnDetail.setOnAction(e -> NavigationManager.getInstance().openAuctionDetail(summary));
    vbox.getChildren().addAll(imagePane, titleLabel, priceLabel, timeLabel, btnDetail);
    return vbox;
  }

  private String timeText(AuctionSummary summary) {
    if (summary.status() == AuctionStatus.OPEN && summary.startTime() != null) {
      return "Bắt đầu sau: " + countdownText(summary.startTime());
    }
    return "Kết thúc: " + (summary.endTime() == null ? "--" : summary.endTime());
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

  private void setupWalletSection() {
    updateBalanceLabel();
  }

  private void updateBalanceLabel() {
    updateBalanceLabel(UserManager.getInstance().getCurrentUser());
  }

  private void updateBalanceLabel(User user) {
    if (balanceLabel == null) {
      return;
    }
    if (user == null) {
      balanceLabel.setText("Số dư: 0 đ");
      return;
    }
    Wallet wallet = user.getWallet();
    BigDecimal total = wallet.getTotalBalance();
    balanceLabel.setText("Số dư: " + formatCurrency(total) + " đ");
  }

  private String formatCurrency(BigDecimal amount) {
    if (amount == null) {
      return "0";
    }
    return currencyFormat.format(amount);
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
      logger.info("Reloading auctions...");
    } catch (Exception e) {
      setReloadLoading(false);
      logger.error("Failed to reload auctions", e);
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
  public void handleAuth(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.USER_PROFILE);
  }

  /** Member. */
  @FXML
  public void switchToLive(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.LIVE);
  }

  /** Member. */
  @FXML
  public void switchToMine(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.HISTORY);
  }

  /** Member. */
  @FXML
  public void switchToMess(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.MESSAGE);
  }

  /** Member. */
  @FXML
  public void switchToOrganize(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.ORGANIZE);
  }

  /** Member. */
  @FXML
  public void switchToDeposit(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.DEPOSIT);
  }

  @FXML
  public void switchToAll(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.ALL_AUCTIONS);
  }

  @Override
  public void cleanup() {
    notifications.removeUpdateListener(summariesListener);
    setReloadLoading(false);
    stopCountdownTimelines();
    for (Timeline timeline : timelines) {
      timeline.stop();
    }
    timelines.clear();
    logger.debug("FirstScene cleaned up");
  }

  private void stopCountdownTimelines() {
    for (Timeline timeline : countdownTimelines) {
      timeline.stop();
    }
    countdownTimelines.clear();
  }
}
