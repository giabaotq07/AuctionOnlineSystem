package app.controllers;

import app.config.AlertUtils;
import app.config.NavigationManager;
import app.config.View;
import app.models.*;
import app.network.Client;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class MyHistoryController {

  @FXML private Stage stage;
  private Scene scene;

  @FXML private ListView<AuctionSession> sessionListView;
  @FXML private ListView<HistoryRecord> historyListView;
  @FXML private TextArea detailArea;

  @FXML
  public void initialize() {

    // load sessions
    sessionListView.getItems().setAll(DataStore.sessions);

    // click session → filter history
    sessionListView.setOnMouseClicked(
        e -> {
          AuctionSession session = sessionListView.getSelectionModel().getSelectedItem();
          if (session == null) return;

          int sid = session.getId();

          historyListView
              .getItems()
              .setAll(HistoryStore.history.stream().filter(h -> h.getSessionId() == sid).toList());
        });

    // click history → detail
    historyListView.setOnMouseClicked(
        e -> {
          HistoryRecord r = historyListView.getSelectionModel().getSelectedItem();
          if (r != null) {
            detailArea.setText(
                "TYPE: " + r.getType() + "\nTIME: " + r.getTime() + "\nMESSAGE: " + r.getMessage());
          }
        });
  }

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
    if (!Client.getInstance().isConnected()) {
      AlertUtils.showError("Mất kết nối", "Bạn đã mất kết nối tới server. Vui lòng kết nối lại!");
      return;
    }
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
