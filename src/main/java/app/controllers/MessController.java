package app.controllers;

import app.config.NavigationManager;
import app.data.ChatRequest;
import app.data.ChatResponse;
import app.data.Response;
import app.data.UserData;
import app.enums.PacketType;
import app.enums.View;
import app.models.PacketReq;
import app.network.Client;
import app.utils.AlertUtils;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class MessController {

  @FXML private TextArea myTextArea;
  @FXML private VBox chatBox;
  @FXML private ScrollPane scrollPane;

  private Client client;
  private Consumer<Response> chatHandler;

  @FXML
  public void initialize() {
    client = Client.getInstance();

    chatBox.heightProperty().addListener((obs, oldVal, newVal) -> scrollPane.setVvalue(1.0d));

    chatHandler =
        response -> {
          if (!(response instanceof ChatResponse)) {
            return;
          }
          ChatResponse chatResponse = (ChatResponse) response;
          String sender = chatResponse.sender();
          Platform.runLater(() -> addBubble(sender, chatResponse.content(), false));
        };
    client.subscribe(PacketType.CHAT, chatHandler);
  }

  boolean isMe(int id) {
    Client client = Client.getInstance();
    return client.getCurrentUser().getId() == id;
  }

  // =========================
  // ADD MESSAGE BUBBLE
  // =========================
  public void addBubble(String sender, String content, boolean isMe) {

    HBox row = new HBox();
    row.setPadding(new Insets(10));
    row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

    VBox messageGroup = new VBox(4);
    messageGroup.setMaxWidth(520);
    messageGroup.setAlignment(isMe ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

    // NAME
    Label nameLabel = new Label(isMe ? "Bạn" : sender);
    nameLabel.setStyle(
            "-fx-text-fill: #94a3b8;" + "-fx-font-size: 11px;" + "-fx-font-weight: bold;");

    // BUBBLE
    TextFlow bubble = getTextFlow(isMe);

    // TEXT PARSE
    String[] words = content.split(" ");

    for (String word : words) {
      Text t = new Text(word + " ");
      t.setFont(Font.font("Segoe UI", 14));
      t.setFill(Color.WHITE);

      if (word.startsWith("@")) {
        t.setFill(Color.web("#fbbf24"));
        t.setStyle("-fx-font-weight: bold; -fx-underline: true;");
      }

      bubble.getChildren().add(t);
    }

    messageGroup.getChildren().addAll(nameLabel, bubble);
    row.getChildren().add(messageGroup);

    chatBox.getChildren().add(row);
  }

  private static TextFlow getTextFlow(boolean isMe) {
    TextFlow bubble = new TextFlow();
    bubble.setPadding(new Insets(10, 14, 10, 14));

    if (isMe) {
      bubble.setStyle(
              "-fx-background-color: #4f46e5;"
                      + "-fx-background-radius: 14 14 4 14;"
                      + "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.25), 10, 0, 0, 3);");
    } else {
      bubble.setStyle(
              "-fx-background-color: #161b26;"
                      + "-fx-background-radius: 14 14 14 4;"
                      + "-fx-border-color: #2d3748;"
                      + "-fx-border-radius: 14 14 14 4;");
    }
    return bubble;
  }

  @FXML
  public void send() {
    String text = myTextArea.getText();
    if (text != null && !text.trim().isEmpty()) {
      ChatRequest chatRequest =
              new ChatRequest(new UserData(client.getCurrentUser()), text, LocalDateTime.now());
      try {
        client.sendRequest(PacketReq.of(PacketType.CHAT, chatRequest));
      } catch (IOException e) {
        AlertUtils.showError("Lỗi Kết nối", "Server không phản hồi");
        return;
      }
      myTextArea.clear();
    }
  }

  @FXML
  public void SwitchToUI(ActionEvent event) {
    if (chatHandler != null) {
      client.unsubscribe(PacketType.CHAT, chatHandler);
    }
    NavigationManager.getInstance().navigateTo(View.UI);
  }
}