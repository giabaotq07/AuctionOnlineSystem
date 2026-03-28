package app.controllers;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UIController {
  @FXML private Stage stage;
  private Scene scene;

  @FXML
  public void SwitchToLive(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/live_auction.fxml"));
    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }

  @FXML
  public void SwitchToMine(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/my_auction.fxml"));
    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }

  @FXML
  public void SwitchToOrganize(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/hold_an_auction.fxml"));
    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }
}
