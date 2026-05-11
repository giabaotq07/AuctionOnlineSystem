package app.network;

import app.data.AuctionsRequest;
import app.data.Response;
import app.enums.PacketType;
import app.exception.AppException;
import app.exception.ConnectException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
  private static volatile Client instance;
  private Socket socket;
  private User currentUser;
  private BufferedWriter writer;
  private BufferedReader reader;
  Map<PacketType, List<Consumer<Response>>> observersMap = new HashMap<>();
  private boolean connected = false;
  Logger logger = LoggerFactory.getLogger(Client.class);

  private Client() {}

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
      while ((line = reader.readLine()) != null) {
        try {
          PacketRes packet = JsonUtil.fromJson(line, PacketRes.class);
          Response response = packet.getData();
          switch (packet.getType()) {
            case PLACE_BID:
              Client.getInstance()
                  .sendRequest(PacketReq.of(PacketType.FETCH_AUCTIONS, new AuctionsRequest()));
            case CREATE_AUCTION:
              Client.getInstance()
                  .sendRequest(PacketReq.of(PacketType.FETCH_AUCTIONS, new AuctionsRequest()));
          }
          if (response != null) {
            notify(packet.getType(), response);
          } else {
            logger.warn("[CLIENT] No response mapping for type: {}", packet.getType());
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

  public void sendRequest(PacketReq packet) throws IOException {
    if (writer != null) {
      String json = JsonUtil.toJson(packet);
      writer.write(json);
      writer.newLine();
      writer.flush();
    }
  }

  public void notify(PacketType packetType, Response response) {
    List<Consumer<Response>> users = observersMap.get(packetType);
    if (users != null) {
      for (Consumer<Response> listener : users) {

        listener.accept(response);
      }
    }
  }

  public void subscribe(PacketType packetType, Consumer<Response> observer) {
    observersMap.computeIfAbsent(packetType, k -> new ArrayList<>()).add(observer);
  }

  public void unsubscribe(PacketType packetType, Consumer<Response> observer) {
    List<Consumer<Response>> users = observersMap.get(packetType);
    if (users != null) {
      users.remove(observer);
    }
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  public void closeResources() {
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
