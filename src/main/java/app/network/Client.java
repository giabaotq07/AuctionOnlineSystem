package app.network;

import app.data.Response;
import app.enums.PacketType;
import app.exception.ConnectException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.utils.JsonUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
  private Thread listenerThread;
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
    listenerThread = new Thread(this::listen, "client-listener");
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
      if (response == null) {
        logger.warn(
            "[CLIENT] No response data for type: {} (data was: {})",
            packet.getType(),
            packet.getRawData());
        return;
      }
      notifyListeners(packet.getType(), response);
    } catch (Exception e) {
      logger.error("[CLIENT] Failed to process packet", e);
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

  /** Member. */
  @SuppressWarnings("unchecked")
  public <T extends Response> void notifyListeners(PacketType packetType, T response) {
    List<PacketListener<?>> listeners = listenersMap.get(packetType);
    if (listeners == null || listeners.isEmpty()) {
      return;
    }
    for (PacketListener<?> listener : listeners) {
      try {
        PacketListener<T> typedListener = (PacketListener<T>) listener;
        typedListener.handle(response);
      } catch (Exception e) {
        logger.error("[CLIENT] Listener error: {}", packetType, e);
      }
    }
  }

  /** subscribe. */
  public <T extends Response> void subscribe(PacketType packetType, PacketListener<T> listener) {
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
    listeners.remove(listener);
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
}
