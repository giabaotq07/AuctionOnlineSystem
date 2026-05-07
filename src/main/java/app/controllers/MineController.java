package app.controllers;

import app.config.NavigationManager;
import java.io.IOException;

import app.enums.View;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class MineController {
  @FXML private Stage stage;

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}
