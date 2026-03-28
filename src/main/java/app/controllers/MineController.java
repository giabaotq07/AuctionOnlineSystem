package app.controllers;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MineController {
  @FXML private Stage stage;
  private Scene scene;

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/user_interface.fxml"));
    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    // String css = this.getClass().getResource("style.css").toExternalForm();
    // scene.getStylesheets().add(css);
    stage.show();
  }
}
