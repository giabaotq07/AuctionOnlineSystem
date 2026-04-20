package app.controllers;

import app.config.NavigationManager;
import app.config.View;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UIController {
  @FXML private Stage stage;
  private Scene scene;

  @FXML
  public void SwitchToLive(ActionEvent event) throws IOException {
    NavigationManager.getInstance().navigateTo(View.LIVE);
  }

  @FXML
  public void SwitchToMine(ActionEvent event) throws IOException {
    NavigationManager.getInstance().navigateTo(View.MINE);
  }

  @FXML
  public void SwitchToOrganize(ActionEvent event) throws IOException {
    NavigationManager.getInstance().navigateTo(View.ORGANIZE);
  }

  @FXML
  public void SwitchToMess(ActionEvent event) throws IOException {
    NavigationManager.getInstance().navigateTo(View.MESSAGE);
  }
}
