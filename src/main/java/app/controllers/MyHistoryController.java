package app.controllers;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import Common.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class MyHistoryController {

  @FXML
  private Stage stage;

  private Scene scene;

  @FXML
  private ListView<AuctionSession> sessionListView;

  @FXML
  private ListView<HistoryRecord> historyListView;

  @FXML
  private TextArea detailArea;

  // FORMAT TIME (clean UI)
  private static final DateTimeFormatter FORMATTER =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  @FXML
  public void initialize() {

    // load sessions
    sessionListView.getItems().setAll(DataStore.sessions);

    // click session → filter history
    sessionListView.setOnMouseClicked(e -> {

      AuctionSession session = sessionListView.getSelectionModel().getSelectedItem();
      if (session == null) return;

      String sid = session.getSessionId();

      historyListView.getItems().setAll(
              HistoryStore.history.stream()
                      .filter(h -> h.getSessionId().equals(sid))
                      .toList()
      );
    });

    // click history → show detail
    historyListView.setOnMouseClicked(e -> {

      HistoryRecord r = historyListView.getSelectionModel().getSelectedItem();
      if (r == null) return;

      detailArea.setText(
              "TYPE: " + r.getType() +
                      "\nTIME: " + r.getTime().format(FORMATTER) +
                      "\nCONTENT: " + r.getMessage()
      );
    });
  }

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {

    FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/app/views/FirstScene.fxml")
    );

    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

    scene = new Scene(loader.load(), 1280, 720);

    stage.setScene(scene);
    stage.show();
  }
}