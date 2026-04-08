package app.controllers;

import Common.core.Message;
import Server.Client;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MessController {
  @FXML private TextArea myTextArea;
  @FXML private VBox chatBox;
  @FXML private ScrollPane scrollPane;

  @FXML
  public void initialize() {
    receive();
    try {
      Client.getInstance().receiveMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
    try {
      Client.getInstance().removeMessageHandler();
    } catch (IOException e) {
      e.printStackTrace();
    }
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/user_interface.fxml"));
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Scene scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    // String css = this.getClass().getResource("style.css").toExternalForm();
    // scene.getStylesheets().add(css);
    stage.show();
  }

  @FXML
  public void send() {
    try {
      Client.getInstance().sendMessages(myTextArea.getText());
      myTextArea.clear();
    } catch (IOException e) {
      //
    }
  }

  public void receive() {
    try {
      Client.getInstance()
          .setMessageHandler((message) -> Platform.runLater(() -> addBubble(message)));
    } catch (IOException e) {
      //
    }
  }

  public void addBubble(Message message) {
    Label label = new Label(message.toString());
    label.setWrapText(true);
    HBox container = new HBox(label);
    chatBox.getChildren().add(container);
  }
}
