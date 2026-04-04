package app.controllers;

import Server.Client;
import Server.ClientHandler;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
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
  @FXML private Stage stage;
  @FXML private TextArea myTextArea;
  @FXML private VBox chatBox;
  @FXML private ScrollPane scrollPane;
  private Scene scene;
  private Client client;
  private ObjectOutputStream output;

  @FXML
  public void SwitchToUI(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/user_interface.fxml"));
    stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    scene = new Scene(loader.load(), 1280, 720);
    stage.setScene(scene);
    close();
    // String css = this.getClass().getResource("style.css").toExternalForm();
    // scene.getStylesheets().add(css);
    stage.show();
  }

  @FXML
  public void writeMessages() {
    String line = myTextArea.getText().trim();
    try {
      System.out.println("Nhập tin nhắn:");
      if (!line.trim().isEmpty()) {
        System.out.println(line);
        addBubble(line);
        output.writeUTF(line);
        output.flush();
      }
    } catch (SocketException e) {
      System.err.println("Server mất kết nối.");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      myTextArea.clear();
    }
  }

  public void addBubble(String text) {
    Label label = new Label(text);
    label.setWrapText(true);
    HBox container = new HBox(label);
    chatBox.getChildren().add(container);
  }

  public void setClient(Client client) {
    this.client = client;
    output = client.getOutput();
  }

  private void close() {
    Socket socket = client.getSocket();
    try {
      output.writeUTF(ClientHandler.STOP_STRING);
      System.out.println("ngat ket noi");
      if (output != null) {
        output.close();
      }
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
