package app.controllers;

import app.config.NavigationManager;
import app.enums.AuctionStatus;
import app.enums.View;
import app.models.Auction;
import app.models.DataStore;
import app.network.Client;
import app.utils.AlertUtils;
import java.io.IOException;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
  @FXML private FlowPane completedAuctionsPane;

  private Timeline autoScroll;

  // FIX: tránh magic number lệch nhau
  private static final double CARD_WIDTH = 280;
  private static final double SPACING = 30;

  private HBox cardContainer;

  @FXML
  public void initialize() {

    if (btnAuth != null) {
      btnAuth.setText(
          DataStore.currentUser != null
              ? "Xin chào, " + DataStore.currentUser.getName()
              : "Đăng nhập / Đăng ký");
    }

    List<Auction> activeS =
        DataStore.sessions.stream().filter(s -> s.getStatus() == AuctionStatus.RUNNING).toList();

    cardContainer = createContainer(activeS);

    // ===== FIX: container không bị co méo =====
    cardContainer.setAlignment(Pos.CENTER_LEFT);
    cardContainer.setSpacing(SPACING);

    ScrollPane viewport = new ScrollPane();
    viewport.setContent(cardContainer);

    viewport.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    viewport.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    viewport.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
    viewport.setFitToHeight(true);

    double viewportWidth = 3 * CARD_WIDTH + 2 * SPACING;
    viewport.setPrefWidth(viewportWidth);
    viewport.setMaxWidth(viewportWidth);
    viewport.setPrefHeight(350);

    if (activeAuctionsPane != null) {
      activeAuctionsPane.getChildren().clear();
      activeAuctionsPane.getChildren().add(viewport);

      // ===== AUTO SCROLL FIXED =====
      autoScroll =
          new Timeline(
              new KeyFrame(
                  Duration.seconds(3),
                  e -> {
                    double contentWidth = cardContainer.getWidth();
                    double viewWidth = viewport.getViewportBounds().getWidth();

                    double maxScroll = contentWidth - viewWidth;
                    if (maxScroll <= 0) return;

                    double currentPixel = viewport.getHvalue() * maxScroll;

                    double step = CARD_WIDTH + SPACING;

                    double nextPixel = currentPixel + step;

                    if (nextPixel >= maxScroll) {
                      nextPixel = 0;
                    }

                    viewport.setHvalue(nextPixel / maxScroll);
                  }));

      autoScroll.setCycleCount(Timeline.INDEFINITE);
      autoScroll.play();

      viewport.setOnMouseEntered(e -> autoScroll.pause());
      viewport.setOnMouseExited(e -> autoScroll.play());
    }

    if (completedAuctionsPane != null) {
      populateCompletedAuctions();
    }

    if (sessionListView != null) {
      sessionListView.getItems().setAll(DataStore.sessions);

      searchField.textProperty().addListener((obs, o, n) -> searchSessions(n));

      Timeline timeline =
          new Timeline(
              new KeyFrame(
                  Duration.seconds(5),
                  e -> {
                    String key = searchField != null ? searchField.getText() : "";

                    if (key == null || key.isBlank()) {
                      sessionListView.getItems().setAll(DataStore.sessions);
                    } else {
                      searchSessions(key);
                    }

                    if (activeAuctionsPane != null && !activeAuctionsPane.getChildren().isEmpty()) {

                      ScrollPane vp = (ScrollPane) activeAuctionsPane.getChildren().get(0);

                      List<Auction> actives =
                          DataStore.sessions.stream()
                              .filter(s -> s.getStatus() == AuctionStatus.RUNNING)
                              .toList();

                      cardContainer = createContainer(actives);
                      vp.setContent(cardContainer);
                    }

                    if (completedAuctionsPane != null) {
                      populateCompletedAuctions();
                    }
                  }));

      timeline.setCycleCount(Timeline.INDEFINITE);
      timeline.play();
    }
  }

  // ================= FIXED CARD CONTAINER =================

  private HBox createContainer(List<Auction> sessions) {
    HBox container = new HBox();

    container.setAlignment(Pos.CENTER_LEFT);
    container.setSpacing(SPACING);

    for (Auction session : sessions) {
      container.getChildren().add(createAuctionCard(session));
    }

    return container;
  }

  // ================= FULL CARD RESTORED =================

  private VBox createAuctionCard(Auction session) {

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

    // ===== IMAGE (RESTORED) =====
    StackPane imagePane = new StackPane();
    imagePane.setPrefHeight(150);
    imagePane.setStyle("-fx-background-color: #2a2f45; -fx-background-radius: 5;");

    Label imgLabel = new Label("Ảnh tài sản");
    imgLabel.setStyle("-fx-text-fill: #aaa;");
    imagePane.getChildren().add(imgLabel);

    // ===== TITLE =====
    Label titleLabel = new Label(session.getItem().getName());
    titleLabel.setWrapText(true);
    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");

    // ===== PRICE =====
    Label priceLabel = new Label(String.format("Giá hiện tại: " + session.getHighestBid() + "đ"));
    priceLabel.setStyle("-fx-text-fill: #e91e63; -fx-font-weight: bold;");

    // ===== TIME =====
    Label timeLabel = new Label("Kết thúc: " + session.getEndTime());
    timeLabel.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 12px;");

    // ===== BUTTON =====
    Button btnDetail = new Button("Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle("-fx-background-color: #673ab7; -fx-text-fill: white;");
    btnDetail.setOnAction(
        e -> {
          try {
            // Gọi hàm để mở màn hình LIVE với dữ liệu của phiên đấu giá hiện tại
            openLiveWithSession(session, e);
          } catch (IOException ex) {
            // Nếu lỗi, nó chỉ in ra Console mà không báo lên giao diện
            ex.printStackTrace();
          }
        });

    vbox.getChildren().addAll(imagePane, titleLabel, priceLabel, timeLabel, btnDetail);

    return vbox;
  }

  // ================= SEARCH (GIỮ NGUYÊN) =================

  private void searchSessions(String keyword) {

    sessionListView.getItems().clear();

    if (keyword == null || keyword.isBlank()) {
      sessionListView.getItems().setAll(DataStore.sessions);
      return;
    }

    String key = keyword.trim().toLowerCase();

    for (Auction s : DataStore.sessions) {
      String item = s.getItemName() != null ? s.getItemName().toLowerCase() : "";
      if (item.contains(key)) {
        sessionListView.getItems().add(s);
      }
    }
  }

  // ================= COMPLETED (GIỮ LOGIC) =================

  private void populateCompletedAuctions() {

    completedAuctionsPane.getChildren().clear();

    List<Auction> completeds =
        DataStore.sessions.stream().filter(s -> s.getStatus() == AuctionStatus.FINISHED).toList();

    for (Auction session : completeds) {

      VBox vbox = new VBox();
      vbox.setPrefWidth(380);
      vbox.setStyle("-fx-background-color: #1a1f35; -fx-padding: 15;");

      Label title = new Label(session.getItem().getName());
      title.setStyle("-fx-text-fill: white;");

      vbox.getChildren().add(title);
      completedAuctionsPane.getChildren().add(vbox);
    }
  }

  // ================= NAV (KHÔNG ĐỤNG) =================

  @FXML
  public void SwitchToLive(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) return;
    if (DataStore.currentUser == null) return;

    NavigationManager.getInstance().navigateTo(View.LIVE);
  }

  @FXML
  public void SwitchToMine(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) return;
    if (DataStore.currentUser == null) return;

    NavigationManager.getInstance().navigateTo(View.MINE);
  }

  @FXML
  public void SwitchToMess(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) return;
    if (DataStore.currentUser == null) return;

    NavigationManager.getInstance().navigateTo(View.MESSAGE);
  }

  @FXML
  public void SwitchToOrganize(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) return;
    if (DataStore.currentUser == null) return;

    NavigationManager.getInstance().navigateTo(View.ORGANIZE);
  }

  @FXML
  public void handleAuth(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }

  @FXML
  public void handleReload(ActionEvent event) {

    String key = searchField != null ? searchField.getText() : "";

    if (sessionListView != null) {
      if (key == null || key.isBlank()) {
        sessionListView.getItems().setAll(DataStore.sessions);
      } else {
        searchSessions(key);
      }
    }

    if (activeAuctionsPane != null && !activeAuctionsPane.getChildren().isEmpty()) {

      ScrollPane vp = (ScrollPane) activeAuctionsPane.getChildren().get(0);

      List<Auction> actives =
          DataStore.sessions.stream().filter(s -> s.getStatus() == AuctionStatus.RUNNING).toList();

      cardContainer = createContainer(actives);
      vp.setContent(cardContainer);
    }

    if (completedAuctionsPane != null) {
      populateCompletedAuctions();
    }
  }

  private void openLiveWithSession(Auction session, javafx.event.Event event) throws IOException {
    // 1. Kiểm tra kết nối mạng
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
      return;
    }

    // 2. Kiểm tra đăng nhập
    if (DataStore.currentUser == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tham gia đấu giá!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }

    // 3. Chuyển sang màn LIVE và truyền dữ liệu phiên đấu giá vào Controller của màn đó
    NavigationManager.getInstance()
        .navigateTo(
            View.LIVE,
            controller -> {
              if (controller instanceof LiveController) {
                ((LiveController) controller).setSession(session);
              }
            });
  }

  private void pauseScroll() {
    if (autoScroll != null) autoScroll.pause();
  }

  private void resumeScroll() {
    if (autoScroll != null) autoScroll.play();
  }
}
