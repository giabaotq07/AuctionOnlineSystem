package app.controllers;

import app.config.NavigationManager;
import app.data.AuctionSummary;
import app.data.AuctionsRequest;
import app.data.AuctionsResponse;
import app.enums.AuctionStatus;
import app.enums.PacketType;
import app.enums.View;
import app.models.Auction;
import app.models.DataStore;
import app.models.Packet;
import app.network.Client;
import app.utils.AlertUtils;
import app.utils.JsonUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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

public class FirstScene {

  @FXML private Stage stage;
  private Scene scene;

  @FXML private TextField searchField;
  @FXML private ListView<Auction> sessionListView;
  @FXML private TextArea detailArea;
  @FXML private Button btnAuth;
  @FXML private StackPane activeAuctionsPane;
  @FXML private StackPane completedAuctionsPane;

  private Timeline autoScroll;

  private final Client client = Client.getInstance();
  private final List<AuctionSummary> summaries = new ArrayList<>();

  // Hằng số kích thước Card
  private static final double CARD_WIDTH = 280;
  private static final double SPACING = 30;

  // 2 HBox cố định để làm "vật chủ" cho ScrollBox
  private final HBox activeBox = new HBox();
  private final HBox completedBox = new HBox();

  @FXML
  public void initialize() {
    setupHBox(activeBox);
    setupHBox(completedBox);

    if (btnAuth != null) {
      btnAuth.setText(
          DataStore.currentUser != null
              ? "Xin chào, " + DataStore.currentUser.getName()
              : "Đăng nhập / Đăng ký");
    }

    Client.getInstance()
        .setOnMessageReceived(
            packet ->
                Platform.runLater(
                    () -> {
                      if (packet.getType() == PacketType.FETCH_AUCTIONS) {
                        AuctionsResponse response =
                            JsonUtil.fromJson(packet.getData(), AuctionsResponse.class);
                        if (response.success() && response.auctions() != null) {
                          summaries.clear();
                          summaries.addAll(response.auctions());
                          DataStore.sessions.clear();
                          for (AuctionSummary summary : summaries) {
                            DataStore.sessions.add(summary.auction());
                          }
                          updateListView();
                        }
                      }
                    }));

    requestAuctions();

    // Hiển thị lên giao diện thông qua ScrollBox
    if (activeAuctionsPane != null) {
      activeAuctionsPane.getChildren().setAll(createScrollBox(activeBox));
    }

    if (completedAuctionsPane != null) {
      completedAuctionsPane.getChildren().setAll(createScrollBox(completedBox));
    }

    // Timeline cập nhật tự động mỗi 5 giây
    if (sessionListView != null) {
      Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> requestAuctions()));
      timeline.setCycleCount(Timeline.INDEFINITE);
      timeline.play();
    }

    if (searchField != null) {
      searchField.textProperty().addListener((obs, o, n) -> searchSessions(n));
    }
  }

  private void requestAuctions() {
    try {
      Client.getInstance()
          .sendRequest(
              new Packet(PacketType.FETCH_AUCTIONS, JsonUtil.toJson(new AuctionsRequest())));
    } catch (IOException e) {
      AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
    }
  }

  private void updateListView() {
    if (sessionListView == null) {
      return;
    }
    String key = (searchField != null) ? searchField.getText() : "";
    if (key == null || key.isBlank()) {
      sessionListView.getItems().setAll(DataStore.sessions);
    } else {
      searchSessions(key);
    }
  }

  private void setupHBox(HBox hbox) {
    hbox.setAlignment(Pos.CENTER_LEFT);
    hbox.setSpacing(SPACING);
  }

  // ================= DÀNH CHO ACTIVE (GIỮ NGUYÊN TÊN) =================

  private HBox createContainer() {
    HBox container = new HBox();
    setupHBox(container);

    for (AuctionSummary summary : summaries) {
      Auction session = summary.auction();
      if (session.getStatus() == AuctionStatus.RUNNING) {
        container.getChildren().add(createAuctionCard(summary));
      }
    }
    return container;
  }

  // ================= DÀNH CHO COMPLETED (GIỮ NGUYÊN TÊN) =================

  private HBox populateCompletedAuctions() {
    HBox container = new HBox();
    setupHBox(container);

    for (AuctionSummary summary : summaries) {
      Auction session = summary.auction();
      if (session.getStatus() == AuctionStatus.FINISHED) {
        container.getChildren().add(createAuctionCard(summary));
      }
    }
    return container;
  }

  // ================= HÀM TẠO SCROLLBOX DÙNG CHUNG =================

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

  // ================= TẠO CARD (GIỮ NGUYÊN LOGIC) =================

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

    long displayPrice = summary.currentPrice();
    Label priceLabel = new Label("Giá hiện tại: " + displayPrice + " đ");
    priceLabel.setStyle("-fx-text-fill: #e91e63; -fx-font-weight: bold;");

    Label timeLabel = new Label("Kết thúc: " + session.getEndTime());
    timeLabel.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 12px;");

    Button btnDetail =
        new Button(session.getStatus() == AuctionStatus.FINISHED ? "Xem kết quả" : "Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle("-fx-background-color: #673ab7; -fx-text-fill: white;");
    btnDetail.setOnAction(
        e -> {
          try {
            openLiveWithSession(session, e);
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        });

    vbox.getChildren().addAll(imagePane, titleLabel, priceLabel, timeLabel, btnDetail);
    return vbox;
  }

  // ================= TÌM KIẾM =================

  private void searchSessions(String keyword) {
    sessionListView.getItems().clear();

    if (keyword == null || keyword.isBlank()) {
      sessionListView.getItems().setAll(DataStore.sessions);
      return;
    }

    String key = keyword.trim().toLowerCase();
    for (AuctionSummary summary : summaries) {
      String itemName = summary.itemName() != null ? summary.itemName().toLowerCase() : "";
      if (itemName.contains(key)) {
        sessionListView.getItems().add(summary.auction());
      }
    }
  }

  // ================= ĐIỀU HƯỚNG & HỖ TRỢ =================

  @FXML
  public void handleReload(ActionEvent event) {
    requestAuctions();
  }

  private void openLiveWithSession(Auction session, javafx.event.Event event) throws IOException {
    if (!Client.getInstance().connected()) {
      AlertUtils.showError("Mất kết nối", "Vui lòng kết nối lại!");
      return;
    }
    if (DataStore.currentUser == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tham gia!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }
    NavigationManager.getInstance()
        .navigateTo(
            View.LIVE,
            controller -> {
              if (controller instanceof LiveController) {
                ((LiveController) controller).setSession(session);
              }
            });
  }

  @FXML
  public void handleAuth(ActionEvent e) {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }

  @FXML
  public void SwitchToLive(ActionEvent e) throws IOException {
    NavigationManager.getInstance().navigateTo(View.LIVE);
  }

  @FXML
  public void SwitchToMine(ActionEvent e) throws IOException {
    NavigationManager.getInstance().navigateTo(View.HISTORY);
  }

  @FXML
  public void SwitchToMess(ActionEvent e) throws IOException {
    NavigationManager.getInstance().navigateTo(View.MESSAGE);
  }

  @FXML
  public void SwitchToOrganize(ActionEvent e) throws IOException {
    NavigationManager.getInstance().navigateTo(View.ORGANIZE);
  }
}
