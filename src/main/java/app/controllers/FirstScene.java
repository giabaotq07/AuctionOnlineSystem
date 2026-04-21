package app.controllers;

import java.io.IOException;

import app.Common.AuctionSession;
import app.Common.DataStore;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FirstScene {

  @FXML private Stage stage;
  private Scene scene;

  @FXML private TextField searchField;
  @FXML private ListView<AuctionSession> sessionListView;
  @FXML private TextArea detailArea; // 👈 thêm

  @FXML
  public void initialize() {

    // load ban đầu
    sessionListView.getItems().setAll(DataStore.sessions);

    // search realtime
    searchField.textProperty().addListener((obs, oldVal, newVal) -> {
      searchSessions(newVal);
    });

    // click session → show detail
    sessionListView.setOnMouseClicked(e -> {
      AuctionSession s = sessionListView.getSelectionModel().getSelectedItem();
      if (s == null) return;

      detailArea.setText(
              "Id: " + s.getSessionId() +
                      "\nPrice: " + s.getCurrentHighestPrice() +
                      "\nitem: " + s.getItem()
      );
    });
  }

  private void searchSessions(String keyword){

    sessionListView.getItems().clear();

    if (keyword == null || keyword.isBlank()){
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

  @FXML
  public void SwitchToLive(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/BidController.fxml"));
    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }

  @FXML
  public void SwitchToMine(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/AuctionController.fxml"));
    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }

  @FXML
  public void SwitchToOrganize(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/MyHistory.fxml"));
    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }
}