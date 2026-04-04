package app.controllers;

import Server.Client;
import java.io.IOException;
import java.io.ObjectOutputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.swing.Timer;

public class ConnectServerController {
  @FXML private Stage stage;
  @FXML private TextArea myTextArea;
  @FXML private VBox chatBox;
  @FXML private ScrollPane scrollPane;
  private Scene scene;
  private Client client;
  private ObjectOutputStream output;
  @FXML private Label statusLabel;
  private Timer hide;

  public void initialize() {
    statusLabel.setVisible(false);
  }

  @FXML
  public void ConnectServer(ActionEvent event) {
    try {
      client.getInstance();
      SwitchToUI(event);
    } catch (Exception e) {
      statusLabel.setVisible(true);
      if (hide != null && hide.isRunning()) {
        hide.stop();
      }
      hide = new Timer(10000, ex -> statusLabel.setVisible(false));
      hide.setRepeats(false);
      hide.start();
    }
  }

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
