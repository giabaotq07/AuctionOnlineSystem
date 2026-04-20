package app.controllers;
import Server .Client;
import app.config.NavigationManager;
import app.config.View;
import app.models.core.Message;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
    NavigationManager.getInstance().navigateTo(View.UI);
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
