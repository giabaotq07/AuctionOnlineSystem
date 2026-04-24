package app.controllers;

import app.models.*;
import java.io.IOException;
import java.time.LocalDateTime;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AuctionController {

  // ===== INPUT =====
  @FXML private TextField IdField;
  @FXML private TextField nameField;
  @FXML private TextField descriptionField;
  @FXML private TextField priceField;
  @FXML private TextField minutesField;

  // ===== UI =====
  @FXML private ListView<AuctionSession> sessionListView;
  @FXML private TextArea outputArea;

  // ===== ADD SESSION =====
  @FXML
  public void handleAdd() {
    try {
      String id = "S" + (sessionListView.getItems().size() + 1);
      Item item =
          new Item(
              id,
              nameField.getText(),
              descriptionField.getText(),
              Double.parseDouble(priceField.getText()),
              Integer.parseInt(minutesField.getText()));

      AuctionSession session =
          new AuctionSession(
              item.getItemId(),
              item,
              new User("s1", "seller"),
              LocalDateTime.now().plusMinutes(Integer.parseInt(minutesField.getText())));
      DataStore.sessions.add(session);

      sessionListView.getItems().add(session);
      HistoryStore.history.add(
          new HistoryRecord(
              session.getSessionId(),
              HistoryType.ADD_ITEM,
              item.getName() + " " + item.getPrice()));

      outputArea.setText("Đã thêm session!");

    } catch (Exception e) {
      outputArea.setText("Lỗi nhập!");
    }
  }

  @FXML
  public void handleBack(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/FirstScene.fxml"));
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Scene scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }

  // ===== CLICK → HIỆN INFO =====
  @FXML
  public void initialize() {
    sessionListView.setOnMouseClicked(
        e -> {
          AuctionSession s = sessionListView.getSelectionModel().getSelectedItem();
          if (s == null) return;

          outputArea.setText(
              "Item: "
                  + s.getItem().getName()
                  + "\nMô tả: "
                  + s.getItem().getDescription()
                  + "\nGiá hiện tại: $"
                  + s.getCurrentHighestPrice());
        });
  }
}
