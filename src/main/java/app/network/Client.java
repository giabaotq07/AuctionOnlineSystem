package app.network;

import app.models.MessagePacket;
import com.google.gson.Gson;
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
    System.out.println("[CLIENT] Đang kết nối...");
    socket = new Socket("127.0.0.1", 5000);
    System.out.println("[CLIENT] Kết nối thành công!");
    out = new PrintWriter(socket.getOutputStream(), true);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    new Thread(this::listen).start();
  }

  private void listen() {
    try {
      String json;
      while ((json = in.readLine()) != null) {
        System.out.println("[Server] " + json);
        // Sử dụng Type-safe hoặc xử lý JSON thô trước khi parse nếu cần
        MessagePacket<?> packet = gson.fromJson(json, MessagePacket.class);

        if (onMessageReceived != null) {
          // Đẩy về cho Controller xử lý
          onMessageReceived.accept(packet);
        }
      }
    } catch (IOException e) {
      System.err.println("Mất kết nối Server.");
    }
  }

  public void sendRequest(MessagePacket<?> packet) {
    if (out != null) out.println(gson.toJson(packet));
  }

  public void setOnMessageReceived(Consumer<MessagePacket<?>> handler) {
    this.onMessageReceived = handler;
  }
}
