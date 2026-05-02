package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.dao.AuctionDAO;
import app.enums.AuctionStatus;
import app.models.Auction;
import app.models.DataStore;
import app.network.Client;

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
  @FXML private StackPane completedAuctionsPane;

  private Timeline autoScroll;

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
                      : "Đăng nhập / Đăng ký"
      );
    }

    // Nạp dữ liệu lần đầu từ DAO vào các HBox
    refreshAllContainers();

    // Hiển thị lên giao diện thông qua ScrollBox
    if (activeAuctionsPane != null) {
      activeAuctionsPane.getChildren().setAll(createScrollBox(activeBox));
    }

    if (completedAuctionsPane != null) {
      completedAuctionsPane.getChildren().setAll(createScrollBox(completedBox));
    }

    // Timeline cập nhật tự động mỗi 5 giây
    if (sessionListView != null) {
      Timeline timeline = new Timeline(
              new KeyFrame(Duration.seconds(5), e -> {
                // 1. Lấy dữ liệu mới nhất từ Database
                List<Auction> latestSessions = AuctionDAO.getInstance().getAllAuction();

                // 2. Cập nhật ListView
                String key = (searchField != null) ? searchField.getText() : "";
                if (key == null || key.isBlank()) {
                  sessionListView.getItems().setAll(latestSessions);
                } else {
                  searchSessions(key);
                }

                // 3. Cập nhật nội dung các HBox (Giữ nguyên instance HBox để không lỗi Scroll)
                refreshAllContainers();
              })
      );
      timeline.setCycleCount(Timeline.INDEFINITE);
      timeline.play();
    }

    if (searchField != null) {
      searchField.textProperty().addListener((obs, o, n) -> searchSessions(n));
    }
  }

  // Hàm bổ trợ để làm mới nội dung bên trong HBox mà không thay đổi Instance
  private void refreshAllContainers() {
    HBox activeTemp = createContainer();
    activeBox.getChildren().setAll(activeTemp.getChildren());

    HBox completedTemp = populateCompletedAuctions();
    completedBox.getChildren().setAll(completedTemp.getChildren());
  }

  private void setupHBox(HBox hbox) {
    hbox.setAlignment(Pos.CENTER_LEFT);
    hbox.setSpacing(SPACING);
  }

  // ================= DÀNH CHO ACTIVE (GIỮ NGUYÊN TÊN) =================

  private HBox createContainer() {
    HBox container = new HBox();
    setupHBox(container);
    List<Auction> sessions = AuctionDAO.getInstance().getAllAuction();

    for (Auction session : sessions) {
      if (session.getStatus() == AuctionStatus.ACTIVE) {
        container.getChildren().add(createAuctionCard(session));
      }
    }
    return container;
  }

  // ================= DÀNH CHO COMPLETED (GIỮ NGUYÊN TÊN) =================

  private HBox populateCompletedAuctions() {
    HBox container = new HBox();
    setupHBox(container);
    List<Auction> sessions = AuctionDAO.getInstance().getAllAuction();

    for (Auction session : sessions) {
      if (session.getStatus() == AuctionStatus.COMPLETED) {
        container.getChildren().add(createAuctionCard(session));
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

    Timeline scrollTimeline = new Timeline(
            new KeyFrame(Duration.seconds(3), e -> {
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
            })
    );
    scrollTimeline.setCycleCount(Timeline.INDEFINITE);
    scrollTimeline.play();

    viewport.setOnMouseEntered(e -> scrollTimeline.pause());
    viewport.setOnMouseExited(e -> scrollTimeline.play());

    return viewport;
  }

  // ================= TẠO CARD (GIỮ NGUYÊN LOGIC) =================

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
                    + "-fx-spacing: 10;"
    );

    StackPane imagePane = new StackPane();
    imagePane.setPrefHeight(150);
    imagePane.setStyle("-fx-background-color: #2a2f45; -fx-background-radius: 5;");
    Label imgLabel = new Label("Ảnh tài sản");
    imgLabel.setStyle("-fx-text-fill: #aaa;");
    imagePane.getChildren().add(imgLabel);

    Label titleLabel = new Label(session.getItem().getName());
    titleLabel.setWrapText(true);
    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");

    Label priceLabel = new Label(String.format("Giá hiện tại: %,.0f đ", session.getCurrentHighestPrice()));
    priceLabel.setStyle("-fx-text-fill: #e91e63; -fx-font-weight: bold;");

    Label timeLabel = new Label("Kết thúc: " + session.getFormatEndTime());
    timeLabel.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 12px;");

    Button btnDetail = new Button(session.getStatus() == AuctionStatus.COMPLETED ? "Xem kết quả" : "Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle("-fx-background-color: #673ab7; -fx-text-fill: white;");
    btnDetail.setOnAction(e -> {
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
    List<Auction> allFromDb = AuctionDAO.getInstance().getAllAuction();

    if (keyword == null || keyword.isBlank()) {
      sessionListView.getItems().setAll(allFromDb);
      return;
    }

    String key = keyword.trim().toLowerCase();
    for (Auction s : allFromDb) {
      String item = s.getItem().getName() != null ? s.getItem().getName().toLowerCase() : "";
      if (item.contains(key)) {
        sessionListView.getItems().add(s);
      }
    }
  }

  // ================= ĐIỀU HƯỚNG & HỖ TRỢ =================

  @FXML
  public void handleReload(ActionEvent event) {
    refreshAllContainers();
    String key = (searchField != null) ? searchField.getText() : "";
    searchSessions(key);
  }

  private void openLiveWithSession(Auction session, javafx.event.Event event) throws IOException {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Vui lòng kết nối lại!");
      return;
    }
    if (DataStore.currentUser == null) {
      AlertUtils.showError("Chưa đăng nhập", "Bạn phải đăng nhập để tham gia!");
      NavigationManager.getInstance().navigateTo(View.LOGIN);
      return;
    }
    NavigationManager.getInstance().navigateTo(View.LIVE, controller -> {
      if (controller instanceof LiveController) {
        ((LiveController) controller).setSession(session);
      }
    });
  }

  @FXML public void handleAuth(ActionEvent e) { NavigationManager.getInstance().navigateTo(View.LOGIN); }
  @FXML public void SwitchToLive(ActionEvent e) throws IOException { NavigationManager.getInstance().navigateTo(View.LIVE); }
  @FXML public void SwitchToMine(ActionEvent e) throws IOException { NavigationManager.getInstance().navigateTo(View.HISTORY); }
  @FXML public void SwitchToMess(ActionEvent e) throws IOException { NavigationManager.getInstance().navigateTo(View.MESSAGE); }
  @FXML public void SwitchToOrganize(ActionEvent e) throws IOException { NavigationManager.getInstance().navigateTo(View.ORGANIZE); }
}