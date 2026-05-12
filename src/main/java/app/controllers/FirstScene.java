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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirstScene {

  @FXML private Stage stage;

  private final Logger logger = LoggerFactory.getLogger(FirstScene.class);

  @FXML private TextField searchField;
  @FXML private ListView<Auction> sessionListView;
  @FXML private TextArea detailArea;
  @FXML private Button btnAuth;
  @FXML private StackPane activeAuctionsPane;
  @FXML private StackPane completedAuctionsPane;

  private final Client client = Client.getInstance();

  // ✔ LOCAL STATE (KHÔNG dùng DataStore để render UI trực tiếp)
  private final List<AuctionSummary> summaries = new ArrayList<>();

  private PacketListener<CreateAuctionResponse> createAuctionHandler;

  private static final double CARD_WIDTH = 280;
  private static final double SPACING = 30;

  private final HBox activeBox = new HBox();
  private final HBox completedBox = new HBox();

  @FXML
  public void initialize() {

    setupHBox(activeBox);
    setupHBox(completedBox);

    // ================= AUTH UI =================
    if (btnAuth != null) {
      btnAuth.setText(
          client.getCurrentUser() != null
              ? "Xin chào, " + client.getCurrentUser().getName()
              : "Đăng nhập / Đăng ký");
    }

    if (activeAuctionsPane != null) {
      activeAuctionsPane.getChildren().setAll(createScrollBox(activeBox));
    }

    if (completedAuctionsPane != null) {
      completedAuctionsPane.getChildren().setAll(createScrollBox(completedBox));
    }

    // ================= RENDER INITIAL UI =================
    createAuctionHandler =
        response ->
            Platform.runLater(
                () -> {
                  if (response == null || response.auction() == null) return;

                  AuctionSummary summary = response.auction();
                  Auction session = summary.auction();

                  // ✔ DEDUPLICATE
                  boolean exists =
                      summaries.stream().anyMatch(s -> s.auction().getId() == session.getId());

                  if (exists) return;

                  summaries.add(summary);

                  // ✔ UPDATE UI INCREMENTALLY (KHÔNG REBUILD FULL)
                  if (session.getStatus() == AuctionStatus.RUNNING) {
                    activeBox.getChildren().add(createAuctionCard(summary));
                  } else {
                    completedBox.getChildren().add(createAuctionCard(summary));
                  }

                  updateListView();
                });

    client.subscribe(PacketType.CREATE_AUCTION, createAuctionHandler);

    Client.getInstance()
        .subscribe(
            PacketType.FETCH_AUCTIONS,
            (AuctionsResponse _) ->
                Platform.runLater(
                    () -> {
                      try {
                        summaries.clear();
                        summaries.addAll(DataStore.getInstance().sessions);
                        rebuildUI();
                      } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                      }
                    }));

    summaries.clear();
    summaries.addAll(DataStore.getInstance().sessions);
    rebuildUI();
    logger.debug("FirstScene initialized");
  }

  // ================= UI BUILD =================

  private void rebuildUI() {
    activeBox.getChildren().clear();
    completedBox.getChildren().clear();

    for (AuctionSummary summary : summaries) {
      Auction session = summary.auction();

      if (session.getStatus() == AuctionStatus.RUNNING) {
        activeBox.getChildren().add(createAuctionCard(summary));
      } else {
        completedBox.getChildren().add(createAuctionCard(summary));
      }
    }

    updateListView();
  }

  private void updateListView() {
    if (sessionListView == null) return;

    sessionListView.getItems().clear();

    String key = searchField != null ? searchField.getText() : "";

    for (AuctionSummary summary : summaries) {
      String itemName = summary.itemName() != null ? summary.itemName().toLowerCase() : "";

      if (key == null || key.isBlank() || itemName.contains(key.toLowerCase())) {
        sessionListView.getItems().add(summary.auction());
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
    viewport.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
    viewport.setFitToHeight(true);

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

                  if (maxScroll <= 0) return;

                  double step = CARD_WIDTH + SPACING;
                  double nextPixel = (viewport.getHvalue() * maxScroll) + step;

                  if (nextPixel >= maxScroll) {
                    nextPixel = 0;
                  }

                  viewport.setHvalue(nextPixel / maxScroll);
                }));

    scrollTimeline.setCycleCount(Timeline.INDEFINITE);
    scrollTimeline.play();

    viewport.setOnMouseEntered(e -> scrollTimeline.pause());
    viewport.setOnMouseExited(e -> scrollTimeline.play());

    return viewport;
  }

  // ================= CARD =================

  private VBox createAuctionCard(AuctionSummary summary) {

    Auction session = summary.auction();

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
    imagePane.setStyle("-fx-background-color: #2a2f45; -fx-background-radius: 5;");

    Label imgLabel = new Label("Ảnh tài sản");
    imgLabel.setStyle("-fx-text-fill: #aaa;");
    imagePane.getChildren().add(imgLabel);

    Label titleLabel = new Label(summary.itemName());
    titleLabel.setWrapText(true);
    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");

    Label priceLabel = new Label("Giá hiện tại: " + summary.currentPrice() + " đ");
    priceLabel.setStyle("-fx-text-fill: #e91e63; -fx-font-weight: bold;");

    Label timeLabel = new Label("Kết thúc: " + session.getEndTime());
    timeLabel.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 12px;");

    Button btnDetail =
        new Button(session.getStatus() == AuctionStatus.FINISHED ? "Xem kết quả" : "Chi tiết");

    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle("-fx-background-color: #673ab7; -fx-text-fill: white;");

    btnDetail.setOnAction(e -> openLiveWithSession(session));

    vbox.getChildren().addAll(imagePane, titleLabel, priceLabel, timeLabel, btnDetail);

    return vbox;
  }

  // ================= NAVIGATION =================

  private void openLiveWithSession(Auction session) {
    try {

      if (!client.connected()) {
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
              c -> {
                if (c instanceof LiveController lc) {
                  lc.setSession(session);
                }
              });

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // ================= ACTIONS =================

  @FXML
  public void handleReload(ActionEvent event) {
    try {
      // ✅ Request dữ liệu mới từ server
      summaries.clear();
      summaries.addAll(DataStore.getInstance().sessions);

      // ✅ Rebuild UI với dữ liệu mới
      rebuildUI();

      logger.info("Thành công", "Đã cập nhật danh sách từ server");
    } catch (IOException e) {
      AlertUtils.showError("Lỗi", "Không thể kết nối server");
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
}
