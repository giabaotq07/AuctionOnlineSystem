package app.controllers;

import app.config.NavigationManager;
import app.enums.View;
import app.models.DataStore;
import app.models.HistoryRecord;
import app.network.Client;
import app.utils.AlertUtils;
import java.io.IOException;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MyHistoryController {

  @FXML private Stage stage;
  private Scene scene;

  // nếu bạn vẫn muốn giữ session list
  @FXML private VBox historyContainerPane;

  private HBox cardContainer;
  private ScrollPane scrollPane;

  private final HistoryDAO historyDAO = HistoryDAO.getInstance();

  @FXML
  public void initialize() {

    // giả sử bạn lấy sessionId từ current user / selected auction
    int sessionId = DataStore.currentSessionId;

    List<HistoryRecord> histories = historyDAO.getHistoryBySession(sessionId);

    cardContainer = createContainer(histories);

    scrollPane = new ScrollPane();
    scrollPane.setContent(cardContainer);

    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setFitToHeight(true);
    scrollPane.setStyle("-fx-background-color: transparent;");

    scrollPane.setPrefHeight(320);
    scrollPane.setPrefWidth(900);

    if (historyContainerPane != null) {
      historyContainerPane.getChildren().clear();
      historyContainerPane.getChildren().add(scrollPane);
    }
  }

  // ================= CARD =================

  private VBox createHistoryCard(HistoryRecord record) {

    VBox box = new VBox();
    box.setPrefWidth(280);
    box.setMinWidth(280);
    box.setMaxWidth(280);

    box.setStyle(
        "-fx-background-color: #1a1f35;"
            + "-fx-background-radius: 8;"
            + "-fx-padding: 12;"
            + "-fx-spacing: 8;");

    Label type = new Label(record.getType().name());
    type.setStyle("-fx-text-fill: #e91e63; -fx-font-weight: bold;");

    Label message = new Label(record.getMessage());
    message.setWrapText(true);
    message.setStyle("-fx-text-fill: white;");

    Label time = new Label(String.valueOf(record.getTime()));
    time.setStyle("-fx-text-fill: #9aa0b4; -fx-font-size: 11px;");

    box.getChildren().addAll(type, message, time);

    return box;
  }

  // ================= CONTAINER =================

  private HBox createContainer(List<HistoryRecord> records) {

    HBox container = new HBox();
    container.setAlignment(Pos.CENTER_LEFT);
    container.setSpacing(30);

    for (HistoryRecord r : records) {
      container.getChildren().add(createHistoryCard(r));
    }

    return container;
  }

  // ================= NAV =================

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {

    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
      return;
    }

    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
