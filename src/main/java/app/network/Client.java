package app.network;

import app.exception.AppException;
import app.models.Packet;
import app.models.User;
import app.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
  private static volatile Client instance;
  private Socket socket;
  private User currentUser;
  private BufferedWriter writer;
  private BufferedReader reader;
  private Consumer<Packet> onMessageReceived;
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
    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    writer.flush();
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    Thread thread = new Thread(this::listen);
    thread.setDaemon(true);
    thread.start();
    isConnected = true;
  }

  private void listen() {
    try {
      String line;
      Packet packet;
      while ((line = reader.readLine()) != null) {
        try {
          packet = JsonUtil.fromJson(line, Packet.class);
          System.out.println("[Server] " + packet);
          switch (packet.getType()) {
            case LOGIN:
              System.out.println("[Server] " + packet);
              if (onMessageReceived != null) {
                onMessageReceived.accept(packet);
              }
              break;
            case CREATE_AUCTION:
            case PLACE_BID:
            case CHAT:
              System.out.println("[Server] " + packet);
              if (onMessageReceived != null) {
                onMessageReceived.accept(packet);
              }
              break;
          }
        } catch (AppException e) {
          System.out.println(e.getMessage());
        }
      }
    } catch (IOException e) {
      System.err.println("Mất kết nối Server.");
      isConnected = false;
    } finally {
      closeResources();
    }
  }

  public boolean isConnected() {
    return isConnected;
  }

  public void sendRequest(Packet packet) {
    if (writer != null) {
      try {
        String json = JsonUtil.toJson(packet);
        writer.write(json);
        writer.newLine();
        writer.flush();
        System.out.println("[Client] " + json);
      } catch (IOException e) {
        System.err.println("Lỗi");
      }
    }
  }

  public void setOnMessageReceived(Consumer<Packet> handler) {
    this.onMessageReceived = handler;
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  private void closeResources() {
    try {
      isConnected = false;
      if (reader != null) reader.close();
      if (writer != null) writer.close();
      if (socket != null) socket.close();
    } catch (IOException e) {
      System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
    }
  }
}
