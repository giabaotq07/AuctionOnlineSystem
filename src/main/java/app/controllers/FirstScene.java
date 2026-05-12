package app.controllers;

import app.config.NavigationManager;
import app.data.AuctionSummary;
import app.data.AuctionsResponse;
import app.data.CreateAuctionResponse;
import app.enums.AuctionStatus;
import app.enums.PacketType;
import app.enums.View;
import app.models.Auction;
import app.models.DataStore;
import app.models.PacketReq;
import app.network.Client;
import app.network.PacketListener;
import app.utils.AlertUtils;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirstScene implements Cleanable {
  private static final Logger logger = LoggerFactory.getLogger(FirstScene.class);
  private static final double CARD_WIDTH = 280;
  private static final double SPACING = 30;
  @FXML private TextField searchField;
  @FXML private ListView<AuctionSummary> sessionListView;
  @FXML private Button btnAuth;
  @FXML private StackPane activeAuctionsPane;
  @FXML private StackPane completedAuctionsPane;
  private final Client client = Client.getInstance();
  private final List<AuctionSummary> summaries = new ArrayList<>();
  private final HBox activeBox = new HBox();
  private final HBox completedBox = new HBox();
  private final List<Timeline> timelines = new ArrayList<>();
  private PacketListener<CreateAuctionResponse> createAuctionHandler;
  private PacketListener<AuctionsResponse> fetchAuctionsHandler;

  @FXML
  public void initialize() {
    setupHBox(activeBox);
    setupHBox(completedBox);
    setupListView();
    setupAuthButton();
    setupSearch();
    setupScrollPanes();
    setupListeners();
    loadInitialData();
    logger.debug("FirstScene initialized");
  }

  // ================= INITIALIZE =================
  private void setupListView() {
    sessionListView.setCellFactory(
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
    sessionListView
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (newVal != null) {
                openAuction(newVal.auction());
              }
            });
  }

  private void setupAuthButton() {
    if (btnAuth == null) return;
    if (client.getCurrentUser() != null) {
      btnAuth.setText("Xin chào, " + client.getCurrentUser().getName());
    } else {
      btnAuth.setText("Đăng nhập / Đăng ký");
    }
  }

  private void setupSearch() {
    if (searchField == null) return;
    searchField
        .textProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              updateListView();
            });
  }

  private void setupScrollPanes() {
    if (activeAuctionsPane != null) {
      activeAuctionsPane.getChildren().setAll(createScrollBox(activeBox));
    }
    if (completedAuctionsPane != null) {
      completedAuctionsPane.getChildren().setAll(createScrollBox(completedBox));
    }
  }

  private void setupListeners() {
    createAuctionHandler =
        response ->
            Platform.runLater(
                () -> {
                  if (response == null || response.auction() == null) {
                    return;
                  }
                  AuctionSummary summary = response.auction();
                  Auction auction = summary.auction();
                  boolean exists =
                      summaries.stream().anyMatch(s -> s.auction().getId() == auction.getId());
                  if (exists) {
                    return;
                  }
                  summaries.add(summary);
                  addAuctionCard(summary);
                  updateListView();
                });
    fetchAuctionsHandler =
        response ->
            Platform.runLater(
                () -> {
                  if (response == null || response.auctions() == null) {
                    return;
                  }
                  summaries.clear();
                  summaries.addAll(response.auctions());
                  rebuildUI();
                });
    client.subscribe(PacketType.CREATE_AUCTION, createAuctionHandler);
    client.subscribe(PacketType.FETCH_AUCTIONS, fetchAuctionsHandler);
  }

  private void loadInitialData() {
    summaries.clear();
    summaries.addAll(DataStore.getInstance().sessions);
    rebuildUI();
    try {
      client.sendRequest(new PacketReq(PacketType.FETCH_AUCTIONS, null));
    } catch (Exception e) {
      logger.error("Failed to fetch auctions", e);
    }
  }

  // ================= UI =================
  private void rebuildUI() {
    activeBox.getChildren().clear();
    completedBox.getChildren().clear();
    for (AuctionSummary summary : summaries) {
      addAuctionCard(summary);
    }
    updateListView();
  }

  private void addAuctionCard(AuctionSummary summary) {
    Auction auction = summary.auction();
    VBox card = createAuctionCard(summary);
    if (auction.getStatus() == AuctionStatus.RUNNING) {
      activeBox.getChildren().add(card);
    } else {
      completedBox.getChildren().add(card);
    }
  }

  private void updateListView() {
    if (sessionListView == null) return;
    sessionListView.getItems().clear();
    String keyword = "";
    if (searchField != null && searchField.getText() != null) {
      keyword = searchField.getText().toLowerCase().trim();
    }
    for (AuctionSummary summary : summaries) {
      String itemName = summary.itemName() == null ? "" : summary.itemName().toLowerCase();
      if (keyword.isBlank() || itemName.contains(keyword)) {
        sessionListView.getItems().add(summary);
      }
    }
  }

  private void setupHBox(HBox hbox) {
    hbox.setAlignment(Pos.CENTER_LEFT);
    hbox.setSpacing(SPACING);
  }

  // ================= SCROLL =================
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

  // ================= CARD =================
  private VBox createAuctionCard(AuctionSummary summary) {
    Auction auction = summary.auction();
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
    imagePane.getChildren().add(imgLabel);
    Label titleLabel = new Label(summary.itemName());
    titleLabel.setWrapText(true);
    titleLabel.setStyle(
        "-fx-font-weight: bold;" + "-fx-font-size: 14px;" + "-fx-text-fill: white;");
    Label priceLabel = new Label("Giá hiện tại: " + summary.currentPrice() + " đ");
    priceLabel.setStyle("-fx-text-fill: #e91e63;" + "-fx-font-weight: bold;");
    Label timeLabel = new Label("Kết thúc: " + auction.getEndTime());
    timeLabel.setStyle("-fx-text-fill: #9aa0b4;" + "-fx-font-size: 12px;");
    Button btnDetail =
        new Button(auction.getStatus() == AuctionStatus.FINISHED ? "Xem kết quả" : "Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle("-fx-background-color: #673ab7;" + "-fx-text-fill: white;");
    btnDetail.setOnAction(e -> openAuction(auction));
    vbox.getChildren().addAll(imagePane, titleLabel, priceLabel, timeLabel, btnDetail);
    return vbox;
  }

  // ================= NAVIGATION =================
  private void openAuction(Auction auction) {
    try {
      if (!client.isConnected()) {
        AlertUtils.showError("Mất kết nối", "Vui lòng kết nối lại!");
        return;
      }
      if (client.getCurrentUser() == null) {
        AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập!");
        NavigationManager.getInstance().navigateTo(View.LOGIN);
        return;
      }
      NavigationManager.getInstance()
          .navigateTo(
              View.LIVE,
              controller -> {
                if (controller instanceof LiveController lc) {
                  lc.setSession(auction);
                }
              });
    } catch (Exception e) {
      logger.error("Failed to open auction", e);
    }
  }

  // ================= ACTIONS =================
  @FXML
  public void handleReload(ActionEvent event) {
    try {
      client.sendRequest(new PacketReq(PacketType.FETCH_AUCTIONS, null));
      logger.info("Reloading auctions...");
    } catch (Exception e) {
      logger.error("Failed to reload auctions", e);
      AlertUtils.showError("Lỗi", e.getMessage());
    }
  }

  @FXML
  public void handleAuth(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }

  @FXML
  public void SwitchToLive(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.LIVE);
  }

  @FXML
  public void SwitchToMine(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.HISTORY);
  }

  @FXML
  public void SwitchToMess(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.MESSAGE);
  }

  @FXML
  public void SwitchToOrganize(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.ORGANIZE);
  }

  // ================= CLEANUP =================
  @Override
  public void cleanup() {
    for (Timeline timeline : timelines) {
      timeline.stop();
    }
    timelines.clear();
    client.unsubscribe(PacketType.CREATE_AUCTION, createAuctionHandler);
    client.unsubscribe(PacketType.FETCH_AUCTIONS, fetchAuctionsHandler);
    logger.debug("FirstScene cleaned up");
  }
}
