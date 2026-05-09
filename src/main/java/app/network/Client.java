package app.network;

import app.exception.AppException;
import app.exception.ConnectException;
import app.models.Packet;
import app.models.User;
import app.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
  private static volatile Client instance;
  private Socket socket;
  private User currentUser;
  private BufferedWriter writer;
  private BufferedReader reader;
  private Consumer<Packet> onMessageReceived;
  private boolean connected = false;
  Logger logger = LoggerFactory.getLogger(Client.class);

  public static Client getInstance() {
    if (instance == null) {
      synchronized (Client.class) {
        if (instance == null) instance = new Client();
      }
    }
    return instance;
  }

  public void connect() throws IOException {
    logger.info("[CLIENT] Đang kết nối...");
    socket = new Socket("127.0.0.1", 5000);
    logger.info("[CLIENT] Kết nối thành công!");
    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    writer.flush();
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    Thread thread = new Thread(this::listen);
    thread.setDaemon(true);
    thread.start();
    connected = true;
  }

  private void listen() {
    try {
      String line;
      Packet packet;
      while ((line = reader.readLine()) != null) {
        try {
          packet = JsonUtil.fromJson(line, Packet.class);
          switch (packet.getType()) {
            case LOGIN:
              if (onMessageReceived != null) {
                onMessageReceived.accept(packet);
              }
              break;
            case CREATE_AUCTION:
            case PLACE_BID:
            case CHAT:
              if (onMessageReceived != null) {
                onMessageReceived.accept(packet);
              }
              break;
          }
        } catch (AppException e) {
          logger.info(e.getMessage());
        }
      }
    } catch (IOException e) {
      connected = false;
      throw new ConnectException("Mất kết nối" + e.getMessage());
    } finally {
      closeResources();
    }
  }

  public boolean connected() {
    return connected;
  }

  public void sendRequest(Packet packet) throws IOException {
    if (writer != null) {
        String json = JsonUtil.toJson(packet);
        writer.write(json);
        writer.newLine();
        writer.flush();
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
      logger.info("[CLIENT] Closing resources");
      connected = false;
      if (reader != null) reader.close();
      if (writer != null) writer.close();
      if (socket != null) socket.close();
    } catch (IOException e) {
      logger.warn("Lỗi khi đóng kết nối: {}", e.getMessage());
    }
  }
}
