package app.network;

import app.models.MessagePacket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
  public static final int PORT = 5000;
  private static Server instance;
  private ServerSocket serverSocket;
  private final ExecutorService clientPool = Executors.newCachedThreadPool();
  private static final Map<Integer, ClientHandler> authenticatedClients = new ConcurrentHashMap<>();

  private Server() {
    try {
      serverSocket = new ServerSocket(PORT);
      System.out.println("[SERVER] Đang chạy tại cổng " + PORT);
    } catch (IOException e) { e.printStackTrace(); }
  }

  public static synchronized Server getInstance() {
    if (instance == null) instance = new Server();
    return instance;
  }

  public void start() {
    while (true) {
      try {
        Socket socket = serverSocket.accept();
        clientPool.execute(new ClientHandler(socket));
      } catch (IOException e) { e.printStackTrace(); }
    }
  }

  public static void registerClient(int userId, ClientHandler handler) {
    authenticatedClients.put(userId, handler);
  }

  public static void removeClient(int userId) {
    authenticatedClients.remove(userId);
  }

  public static void broadcast(MessagePacket<?> packet) {
    authenticatedClients.values().forEach(h -> h.sendMessage(packet));
  }

  static void main() {
    Server server = new Server();
    server.start();
  }
}