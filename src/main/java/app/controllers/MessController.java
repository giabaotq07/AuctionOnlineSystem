package app.controllers;

import app.enums.CommandType;
import app.models.MessagePacket;
import app.network.Client;
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
    chatBox
        .heightProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              scrollPane.setVvalue(1.0d);
            });

    // 3. Đăng ký lắng nghe phản hồi từ Server
    setupNetworkListener();
  }

  private void setupNetworkListener() {
    client.setOnMessageReceived(
        packet -> {
          // Dùng Platform.runLater để đảm bảo an toàn cho UI
          Platform.runLater(
              () -> {
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
    Platform.runLater(
        () -> {
          // Lấy tên người gửi (nếu null thì hiện Hệ thống)
          String name = (packet.getMessage() != null) ? packet.getMessage() : "Hệ thống";

          // Lấy nội dung tin nhắn
          String msg = (packet.getData() != null) ? packet.getData().toString() : "";

          // Tạo một Label duy nhất theo định dạng [name]: msg
          Label line = new Label("[" + name + "]: " + msg);
          line.setWrapText(true);

          // Thêm trực tiếp vào chatBox
          chatBox.getChildren().add(line);
        });
  }

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/firstscene.fxml"));
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    Scene scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    String css = this.getClass().getResource("style.css").toExternalForm();
    scene.getStylesheets().add(css);
    stage.show();
  }
}
