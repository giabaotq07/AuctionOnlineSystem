package app.controllers;

import app.enums.CommandType;
import app.models.MessagePacket;
import app.network.Client;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MessController {

    @FXML private TextArea myTextArea;
    @FXML private VBox chatBox;
    @FXML private ScrollPane scrollPane;

    private Client client;

    @FXML
    public void initialize() {
        client = Client.getInstance();

        // auto scroll xuống cuối
        chatBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane.setVvalue(1.0d);
        });

        // 🔥 ONLY HANDLE OTHER USERS MESSAGE
        client.setOnMessageReceived(packet -> {
            if (packet.getType() == CommandType.CHAT) {

                String sender = packet.getMessage();
                String myName = client.getMyUsername();

                // ❌ IGNORE message của chính mình từ server
                if (sender != null && sender.equals(myName)) {
                    return;
                }

                Platform.runLater(() ->
                        addBubble(sender, (String) packet.getData(), false)
                );
            }
        });
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
                "-fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;"
        );

        // BUBBLE
        TextFlow bubble = new TextFlow();
        bubble.setPadding(new Insets(10, 14, 10, 14));

        if (isMe) {
            bubble.setStyle(
                    "-fx-background-color: #4f46e5;" +
                            "-fx-background-radius: 14 14 4 14;" +
                            "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.25), 10, 0, 0, 3);"
            );
        } else {
            bubble.setStyle(
                    "-fx-background-color: #161b26;" +
                            "-fx-background-radius: 14 14 14 4;" +
                            "-fx-border-color: #2d3748;" +
                            "-fx-border-radius: 14 14 14 4;"
            );
        }

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

    // =========================
    // SEND MESSAGE
    // =========================
    @FXML
    public void send() {
        String text = myTextArea.getText();

        if (text != null && !text.trim().isEmpty()) {

            String myName = client.getMyUsername();

            // 🔥 render ngay bên phải (local)
            addBubble(myName, text, true);

            // gửi server
            client.sendRequest(new MessagePacket<>(CommandType.CHAT, text));

            myTextArea.clear();
        }
    }

    // =========================
    // SWITCH UI
    // =========================
    @FXML
    public void SwitchToUI(ActionEvent event) throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/firstscene.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(loader.load(), 1280, 720);

        java.net.URL cssResource = getClass().getResource("/app/views/style.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        stage.setScene(scene);
        stage.show();
    }
}