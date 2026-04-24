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
  private boolean isConnected = false;

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
    Thread thread = new Thread(this::listen);
    thread.setDaemon(true);
    thread.start();
    isConnected = true;
  }

  private void listen() {
    try {
      String line;
      while ((line = in.readLine()) != null) {
        System.out.println("[Server] " + line);
        MessagePacket<?> packet = gson.fromJson(line, MessagePacket.class);

        if (onMessageReceived != null) {
          // Đẩy về cho Controller xử lýw
          onMessageReceived.accept(packet);
        }
      }
    } catch (IOException e) {
      System.err.println("Mất kết nối Server.");
      isConnected = false;
    }
  }

  public boolean isConnected() {
    return isConnected;
  }

  public void sendRequest(MessagePacket<?> packet) {
    if (out != null) out.println(gson.toJson(packet));
  }

  public void setOnMessageReceived(Consumer<MessagePacket<?>> handler) {
    this.onMessageReceived = handler;
  }
}
