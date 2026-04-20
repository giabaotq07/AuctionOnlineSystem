package app.network;

import app.models.MessagePacket;
import com.google.gson.Gson;
import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
  private static volatile Client instance;
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private final Gson gson = new Gson();
  private Consumer<MessagePacket<?>> onMessageReceived;

  public static Client getInstance() {
    if (instance == null) {
      synchronized (Client.class) {
        if (instance == null) instance = new Client();
      }
    }
    return instance;
  }

  public void connect() throws IOException {
    socket = new Socket("127.0.0.1", 5000);
    out = new PrintWriter(socket.getOutputStream(), true);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    new Thread(this::listen).start();
  }

  private void listen() {
    try {
      String json;
      while ((json = in.readLine()) != null) {
        MessagePacket<?> packet = gson.fromJson(json, MessagePacket.class);
        if (onMessageReceived != null) {
          Platform.runLater(() -> onMessageReceived.accept(packet));
        }
      }
    } catch (IOException e) { e.printStackTrace(); }
  }

  public void sendRequest(MessagePacket<?> packet) {
    if (out != null) out.println(gson.toJson(packet));
  }

  public void setOnMessageReceived(Consumer<MessagePacket<?>> handler) {
    this.onMessageReceived = handler;
  }
}