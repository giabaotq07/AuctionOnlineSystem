package app.controllers;

import app.network.Client;
import app.models.CommandType;
import app.models.MessagePacket;
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

  private Client client;

  @FXML
  public void initialize() {
    client = Client.getInstance();
      // 2. Tự động cuộn xuống khi có tin nhắn mới
      chatBox.heightProperty().addListener((observable, oldValue, newValue) -> {
        scrollPane.setVvalue(1.0d);
      });

      // 3. Đăng ký lắng nghe phản hồi từ Server
      setupNetworkListener();
  }

  private void setupNetworkListener() {
    client.setOnMessageReceived(packet -> {
      // Dùng Platform.runLater để đảm bảo an toàn cho UI
      Platform.runLater(() -> {
        addBubble(packet);
        // Hoặc kiểm tra switch-case nếu bạn muốn xử lý riêng từng loại
      });
    });
  }

  @FXML
  public void send() {
    String text = myTextArea.getText();
    if (text != null && !text.trim().isEmpty()) {
      // Nếu muốn gửi tin nhắn chat:
      client.sendRequest(new MessagePacket<>(CommandType.CHAT, text));
      myTextArea.clear();
    }
  }

  public void addBubble(MessagePacket<?> packet) {
    Platform.runLater(() -> {
      VBox messageGroup = new VBox(2); // Khoảng cách giữa tên và nội dung

      // 1. Tên người gửi (Hiện phía trên tin nhắn)
      Label senderLabel = new Label(packet.getMessage() != null ? packet.getMessage() : "Hệ thống");
      senderLabel.setStyle("-fx-text-fill: #8e8e8e; -fx-font-size: 11px; -fx-padding: 0 5 0 5;");

      // 2. Nội dung tin nhắn
      Label contentLabel = new Label(packet.getData().toString());
      contentLabel.setWrapText(true);
      contentLabel.setMaxWidth(350);

      // Style cho bong bóng tin nhắn màu tối
      String bubbleStyle = "-fx-background-radius: 12; -fx-padding: 10; -fx-font-size: 14px; -fx-text-fill: white;";

      if (packet.getType() == CommandType.CHAT) {
        bubbleStyle += "-fx-background-color: #3d3d3d;"; // Màu xám tối cho người khác
      } else if (packet.getType() == CommandType.UPDATE_PRICE) {
        bubbleStyle += "-fx-background-color: #1a4d2e; -fx-border-color: #2ecc71; -fx-border-radius: 12;"; // Màu xanh lá tối cho đấu giá
      } else {
        bubbleStyle += "-fx-background-color: #2c3e50;"; // Màu mặc định
      }

      contentLabel.setStyle(bubbleStyle);

      messageGroup.getChildren().addAll(senderLabel, contentLabel);

      HBox container = new HBox(messageGroup);
      container.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));

      chatBox.getChildren().add(container);
    });
  }

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/user_interface.fxml"));
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Scene scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    stage.show();
  }
}