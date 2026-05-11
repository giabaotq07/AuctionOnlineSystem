package app.network;

import app.models.PacketRes;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);
  public static final int PORT = 5000;
  private static Server instance;
  private ServerSocket serverSocket;
  private final ExecutorService clientPool = Executors.newCachedThreadPool();
  private static final Map<Integer, ClientHandler> authenticatedClients = new ConcurrentHashMap<>();

  private Server() {
    try {
      serverSocket = new ServerSocket();
      // Cho phép tái sử dụng port ngay lập tức (tránh "Address already in use")
      serverSocket.setReuseAddress(true);
      serverSocket.bind(new java.net.InetSocketAddress(PORT));
      logger.info("[SERVER] Đang chạy tại cổng {}", PORT);
    } catch (IOException e) {
      logger.error("[SERVER] Lỗi khởi tạo ServerSocket tại port {}: {}", PORT, e.getMessage());
      throw new RuntimeException("Failed to start server on port " + PORT, e);
    }
  }

  public static synchronized Server getInstance() {
    if (instance == null) instance = new Server();
    return instance;
  }

  public void start() {
    while (true) {
      try {
        Socket socket = serverSocket.accept();
        logger.info("[SERVER] Client mới kết nối: {}", socket.getRemoteSocketAddress());
        clientPool.execute(new ClientHandler(socket));
      } catch (IOException e) {
        if (!serverSocket.isClosed()) {
          logger.error("[SERVER] Lỗi khi nhận connection: {}", e.getMessage());
        }
      }
    }
  }

  public void shutdown() {
    logger.info("[SERVER] Shutting down server...");
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
      clientPool.shutdownNow();
      logger.info("[SERVER] Server shutdown complete");
    } catch (IOException e) {
      logger.error("[SERVER] Error during shutdown: {}", e.getMessage());
    }
  }

  public static void registerClient(int userId, ClientHandler handler) {
    authenticatedClients.put(userId, handler);
  }

  public static void removeClient(int userId) {
    authenticatedClients.remove(userId);
  }

  public static void broadcast(PacketRes packet, int excludeUser) {
    authenticatedClients.values().stream()
        .filter(h -> h.getUser().getId() != excludeUser) // Lọc bỏ người gửi
        .forEach(h -> h.sendMessage(packet));
  }
}
