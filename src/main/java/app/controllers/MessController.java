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
    // 1. Lấy instance và kết nối
    client = Client.getInstance();
    try {
      client.connect();

      // 2. Tự động cuộn xuống khi có tin nhắn mới
      chatBox.heightProperty().addListener((observable, oldValue, newValue) -> {
        scrollPane.setVvalue(1.0d);
      });

      // 3. Đăng ký lắng nghe phản hồi từ Server
      setupNetworkListener();

    } catch (IOException e) {
      System.err.println("Không thể kết nối đến Server: " + e.getMessage());
    }
  }

  private void setupNetworkListener() {
    client.setOnMessageReceived(packet -> {
      // Lưu ý: Client.java đã bọc Platform.runLater,
      // nên ở đây ta có thể trực tiếp thao tác với UI
      switch (packet.getType()) {
        case UPDATE_PRICE:
          // Ví dụ: Hiển thị giá mới vào khung chat hoặc log
          addBubble(new MessagePacket<>(CommandType.UPDATE_PRICE, "Giá mới: " + packet.getData()));
          break;
        case SUCCESS:
          addBubble(packet);
          break;
        case ERROR:
          // Hiển thị lỗi từ Server (ví dụ: đặt giá thấp hơn giá hiện tại)
          System.err.println("Lỗi: " + packet.getMessage());
          break;
      }
    });
  }

  @FXML
  public void send() {
    String text = myTextArea.getText();
    if (text != null && !text.trim().isEmpty()) {
      // Gửi yêu cầu đặt giá hoặc gửi tin nhắn tùy theo CommandType
      client.sendRequest(new MessagePacket<>(CommandType.PLACE_BID, text));
      myTextArea.clear();
    }
  }

  public void addBubble(MessagePacket<?> messagePacket) {
    // Tạo label hiển thị nội dung tin nhắn
    Label label = new Label(messagePacket.getType() + ": " + messagePacket.getData());
    label.setWrapText(true);
    label.getStyleClass().add("chat-label"); // Bạn có thể thêm CSS

    HBox container = new HBox(label);
    // Tùy chỉnh style dựa trên loại tin nhắn (nếu cần)
    chatBox.getChildren().add(container);
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