package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.models.AuctionSession;
import app.models.DataStore;
import app.network.Client;
import java.io.IOException;
import java.util.List;

import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FirstScene {

  @FXML private Stage stage;
  private Scene scene;

  @FXML private TextField searchField;
  @FXML private ListView<AuctionSession> sessionListView;
  @FXML private TextArea detailArea;
  @FXML private Button btnAuth;
  @FXML private StackPane activeAuctionsPane;
  private javafx.animation.Timeline autoScroll;

  @FXML
  public void initialize() {
    if (btnAuth != null) {
      if (DataStore.currentUser != null) {
        btnAuth.setText("Xin chào, " + DataStore.currentUser.getName());
      } else {
        btnAuth.setText("Đăng nhập / Đăng ký");
      }
    }

    HBox cardContainer = createContainer(DataStore.sessions);
    ScrollPane viewport = new ScrollPane();
    viewport.setContent(cardContainer);
    //An thanh cuon
    viewport.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    viewport.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    viewport.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
    viewport.setPrefWidth(900);
    viewport.setPrefHeight(350);

    if (activeAuctionsPane != null) {
      activeAuctionsPane.getChildren().clear();
      activeAuctionsPane.getChildren().add(viewport);
      autoScroll = new javafx.animation.Timeline(
              new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3),e -> {
                double currentH = viewport.getHvalue();
                int totalSessions = DataStore.sessions.size();
                int steps = totalSessions - 3;
                if (steps <=0) return ;
                double stepSize = 1.0/steps;
                if (currentH >= 0.98) {
                  viewport.setHvalue(0); // Quay về đầu
                } else {
                  // Cộng thêm một bước, nhưng không vượt quá 1.0
                  viewport.setHvalue(Math.min(1.0, currentH + stepSize));
                }
              })
      );
      autoScroll.setCycleCount(Timeline.INDEFINITE);
      autoScroll.play();
      viewport.setOnMouseEntered(event -> {
        if (autoScroll != null) autoScroll.pause();
      });

      viewport.setOnMouseExited(event -> {
        if (autoScroll != null) autoScroll.play();
      });
    }

    // load ban đầu

    if (sessionListView != null) {
      sessionListView.getItems().setAll(DataStore.sessions);

      // search realtime
      if (searchField != null) {
        searchField
            .textProperty()
            .addListener(
                (obs, oldVal, newVal) -> {
                  searchSessions(newVal);
                });
      }

      // click session → show detail
      sessionListView.setOnMouseClicked(
          e -> {
            AuctionSession s = sessionListView.getSelectionModel().getSelectedItem();
            if (s == null) return;

            if (detailArea != null) {
              detailArea.setText(
                  "Id: "
                      + s.getId()
                      + "\nPrice: "
                      + s.getCurrentHighestPrice()
                      + "\nitem: "
                      + s.getItem());
            }
          });
    }
  }

  private void searchSessions(String keyword) {

    sessionListView.getItems().clear();

    if (keyword == null || keyword.isBlank()) {
      sessionListView.getItems().setAll(DataStore.sessions);
      return;
    }

    String key = keyword.trim().toLowerCase();

    for (AuctionSession s : DataStore.sessions) {

      String item = "";

      if (s.getItemname() != null) {
        item = s.getItemname().toLowerCase();
      }

      if (item.contains(key)) {
        sessionListView.getItems().add(s);
      }
    }
  }

  private void openLiveWithSession(app.models.AuctionSession session, javafx.event.Event event)
      throws IOException {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
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

  private VBox createAuctionCard(AuctionSession session) {
    VBox vbox = new VBox();
    vbox.setPrefWidth(280.0);
    vbox.setStyle(
        "-fx-background-color:#161b26; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4); -fx-padding: 15; -fx-spacing: 10;");

    StackPane imagePane = new StackPane();
    imagePane.setPrefHeight(150.0);
    imagePane.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 5;");
    Label imgLabel = new Label("Ảnh tài sản");
    imgLabel.setStyle("-fx-text-fill: #aaa;");
    imagePane.getChildren().add(imgLabel);

    Label titleLabel = new Label(session.getItem().getName());
    titleLabel.setWrapText(true);
    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");

    Label priceLabel =
        new Label(String.format("Giá khởi điểm: %,.0f đ", session.getItem().getStartingPrice()));
    priceLabel.setStyle("-fx-text-fill: #673ab7; -fx-font-weight: bold;");

    Label timeLabel = new Label("Kết thúc: " + session.getEndTime().toString());
    timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

    Button btnDetail = new Button("Chi tiết");
    btnDetail.setMaxWidth(Double.MAX_VALUE);
    btnDetail.setStyle(
        "-fx-background-color: #673ab7; -fx-text-fill: white; -fx-background-radius: 4;");
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
  private HBox createContainer(List<AuctionSession> sessions){
    HBox container = new HBox();
    // Căn space
    container.setSpacing(36);
    // Căn lề
    container.setAlignment(Pos.CENTER_LEFT);
    for (AuctionSession session : sessions){
      VBox card = createAuctionCard(session);
      container.getChildren().add(card);
    }
    return container;
  }


  @FXML
  public void SwitchToLive(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
      return;
    }
    NavigationManager.getInstance().navigateTo(View.LIVE);
  }

  @FXML
  public void SwitchToMine(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
      return;
    }
    NavigationManager.getInstance().navigateTo(View.MINE);
  }

  @FXML
  public void SwitchToMess(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
      return;
    }
    NavigationManager.getInstance().navigateTo(View.MESSAGE);
  }

  @FXML
  public void SwitchToOrganize(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
      return;
    }
    NavigationManager.getInstance().navigateTo(View.ORGANIZE);
  }

  @FXML
  public void handleAuth(ActionEvent event) {
    NavigationManager.getInstance().navigateTo(View.LOGIN);
  }
  private void pauseScroll() {
    if (autoScroll != null) {
      autoScroll.pause();
    }
  }

  private void resumeScroll() {
    if (autoScroll != null) {
      autoScroll.play();
    }
  }
}
