package Server;

import app.models.core.Message;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lớp Server để lắng nghe và chấp nhận các kết nối từ Client. */
public class Server {
  /** Cổng mặc định của Server. */
  public static final int PORT = 5000;

  public static final String STOP_STRING = "STOP";
  private static final Map<String, ClientHandler> clientHandlers = new ConcurrentHashMap<>();
  private static final ExecutorService broadcastPool = Executors.newFixedThreadPool(10);
  private final ExecutorService clientPool = Executors.newCachedThreadPool();
  private ServerSocket server;
  private boolean isRunning = true;

  /** Khởi tạo Server và bắt đầu lắng nghe kết nối. */
  public Server() {
    try {
      server = new ServerSocket(PORT);
      System.out.println("Server đang chạy tại cổng " + PORT + "...");
      initConnections();
    } catch (IOException e) {
      System.err.println("Không thể khởi động Server: " + e.getMessage());
    } finally {
      stopServer();
    }
  }

  public static void broadcast(Message message) {
    broadcastPool.submit(
        () ->
            clientHandlers.forEach(
                (id, handler) -> {
                  try {
                    handler.sendMessage(message);
                  } catch (Exception e) {
                    removeClient(id);
                  }
                }));
  }

  private void initConnections() {
    try {
      while (isRunning) {
        Socket clientSocket = server.accept();
        String handlerId = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
        System.out.println("\n[NEW] Client mới kết nối: " + handlerId);
        ClientHandler handler = new ClientHandler(clientSocket, handlerId);
        clientHandlers.put(handlerId, handler);
        clientPool.execute(handler);
      }
    } catch (IOException e) {
      if (isRunning) System.err.println("Lỗi chấp nhận kết nối: " + e.getMessage());
    }
  }

  public static void removeClient(String handlerId) {
    ClientHandler handler = clientHandlers.remove(handlerId);
    if (handler != null) {
      System.out.println("[DISCONNECT] " + handlerId + " đã rời phòng.");
    }
  }

  public void stopServer() {
    isRunning = false;
    try {
      if (server != null) server.close();
      broadcastPool.shutdown();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static void main() {
    new Server();
  }
}
