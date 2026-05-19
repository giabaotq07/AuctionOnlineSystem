package app.client;

import app.client.manager.DataStore;
import app.common.dto.*;
import app.common.enums.PacketType;
import app.common.exception.ConnectException;
import app.common.mapper.DtoMapper;
import app.common.models.PacketReq;
import app.common.models.PacketRes;
import app.common.models.User;
import app.common.observer.PacketListener;
import app.common.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client. */
public class Client {
  private static final Logger logger = LoggerFactory.getLogger(Client.class);
  private static volatile Client instance;
  private static final String HOST = "127.0.0.1";
  private static final int PORT = 5000;
  private Socket socket;
  private BufferedWriter writer;
  private BufferedReader reader;
  private volatile boolean connected = false;
  private volatile boolean closed = true;
  private volatile User currentUser;
  private final Map<PacketType, CopyOnWriteArrayList<PacketListener<?>>> listenersMap =
      new ConcurrentHashMap<>();

  private Client() {}

  /** getInstance. */
  public static Client getInstance() {
    if (instance == null) {
      synchronized (Client.class) {
        if (instance == null) {
          instance = new Client();
        }
      }
    }
    return instance;
  }

  /** connect. */
  public synchronized void connect() throws IOException {
    if (connected) {
      return;
    }
    logger.info("[CLIENT] Connecting to server...");
    closed = false;
    socket = new Socket(HOST, PORT);
    socket.setKeepAlive(true);
    socket.setTcpNoDelay(true);
    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    connected = true;
    startListener();
    logger.info("[CLIENT] Connected successfully");
  }

  private void startListener() {
    Thread listenerThread = new Thread(this::listen, "client-listener");
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  private void listen() {
    try {
      while (!closed && connected && socket != null && !socket.isClosed()) {
        String line = reader.readLine();
        if (line == null) {
          logger.warn("[CLIENT] Server disconnected");
          break;
        }
        handlePacket(line);
      }
    } catch (SocketException e) {
      if (!closed) {
        logger.warn("[CLIENT] Socket closed");
      }
    } catch (IOException e) {
      if (!closed) {
        logger.error("[CLIENT] Connection lost", e);
      }
    } finally {
      closeResources();
    }
  }

  private void handlePacket(String json) {
    try {
      PacketRes packet = JsonUtil.fromJson(json, PacketRes.class);
      if (packet == null || packet.getType() == null) {
        logger.warn("[CLIENT] Invalid packet received");
        return;
      }
      logger.debug(
          "[CLIENT] Received packet type: {}, data: {}", packet.getType(), packet.getRawData());
      Response response = packet.getData();
      if (!packet.isSuccess()) {
        notifyListeners(packet.getType(), response, false, packet.getMessage());
        return;
      }
      if (response == null && packet.getType().resClass != Void.class) {
        logger.warn(
            "[CLIENT] No response data for type: {} (data was: {})",
            packet.getType(),
            packet.getRawData());
        notifyListeners(packet.getType(), response, true, packet.getMessage());
        return;
      }
      updateSessionState(response);
      if (response instanceof AuctionSummariesResponse) {
        DataStore.getInstance()
            .handleSummaryResponse(
                (AuctionSummariesResponse) response, packet.isSuccess(), packet.getMessage());
      }
      if (response instanceof AuctionHistoryResponse) {
        DataStore.getInstance()
            .handleHistoryResponse(
                (AuctionHistoryResponse) response, packet.isSuccess(), packet.getMessage());
      }
      notifyListeners(packet.getType(), response, true, packet.getMessage());
    } catch (Exception e) {
      logger.error("[CLIENT] Failed to process packet", e);
    }
  }

  private void updateSessionState(Response response) {
    if (response instanceof WalletUpdateResponse walletUpdate && walletUpdate.user() != null) {
      updateCurrentUser(walletUpdate.user());
    }
  }

  /** sendRequest. */
  public synchronized void sendRequest(PacketReq packet) throws IOException {
    if (!connected || closed || writer == null) {
      throw new ConnectException("Chưa kết nối tới server");
    }
    if (packet == null) {
      throw new IllegalArgumentException("Packet cannot be null");
    }
    String json = JsonUtil.toJson(packet);
    writer.write(json);
    writer.newLine();
    writer.flush();
  }

  @SuppressWarnings("unchecked")
  private void notifyListeners(
      PacketType packetType, Response response, boolean success, String message) {
    List<PacketListener<?>> listeners = listenersMap.get(packetType);
    if (listeners == null || listeners.isEmpty()) {
      return;
    }
    for (PacketListener<?> listener : listeners) {
      try {
        ((PacketListener<Response>) listener).handle(response, success, message);
      } catch (Exception e) {
        logger.error("[CLIENT] Listener error: {}", packetType, e);
      }
    }
  }

  /** subscribe. */
  public <T extends Response> void subscribe(
      PacketType packetType, Class<T> responseClass, PacketListener<T> listener) {
    validateSubscription(packetType, responseClass, listener);
    listenersMap
        .computeIfAbsent(packetType, k -> new CopyOnWriteArrayList<>())
        .addIfAbsent(listener);
  }

  /** unsubscribe. */
  public <T extends Response> void unsubscribe(PacketType packetType, PacketListener<T> listener) {
    List<PacketListener<?>> listeners = listenersMap.get(packetType);
    if (listeners == null) {
      return;
    }
    listeners.removeIf(registered -> registered == listener || registered.equals(listener));
    if (listeners.isEmpty()) {
      listenersMap.remove(packetType);
    }
  }

  /** clearListeners. */
  public void clearListeners() {
    listenersMap.clear();
  }

  /** closeResources. */
  public synchronized void closeResources() {
    if (closed) {
      return;
    }
    logger.info("[CLIENT] Closing resources");
    closed = true;
    connected = false;
    currentUser = null;
    try {
      if (socket != null && !socket.isClosed()) {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
      }
    } catch (IOException e) {
      logger.warn("[CLIENT] Error closing socket", e);
    }
    try {
      if (reader != null) {
        reader.close();
      }
    } catch (IOException e) {
      logger.warn("[CLIENT] Error closing reader", e);
    }
    try {
      if (writer != null) {
        writer.close();
      }
    } catch (IOException e) {
      logger.warn("[CLIENT] Error closing writer", e);
    }
    socket = null;
    reader = null;
    writer = null;
  }

  public boolean isConnected() {
    return connected;
  }

  public boolean isClosed() {
    return closed;
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(User currentUser) {
    this.currentUser = currentUser;
  }

  /** updateCurrentUser. */
  public void updateCurrentUser(UserData userData) {
    if (userData == null) {
      return;
    }
    currentUser = DtoMapper.toUser(userData);
  }

  private static <T extends Response> void validateSubscription(
      PacketType packetType, Class<T> responseClass, PacketListener<T> listener) {
    if (packetType == null) {
      throw new IllegalArgumentException("Packet type cannot be null");
    }
    if (responseClass == null) {
      throw new IllegalArgumentException("Response class cannot be null");
    }
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    if (!packetType.resClass.equals(responseClass)) {
      throw new IllegalArgumentException(
          "Response class does not match packet type: " + packetType);
    }
  }
}
