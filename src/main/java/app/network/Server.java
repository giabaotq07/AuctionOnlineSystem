package app.network;

import app.dao.*;
import app.dao.impl.*;
import app.models.PacketRes;
import app.service.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);
  public static final int PORT = 5000;
  private static volatile Server instance;
  private ServerSocket serverSocket;
  private volatile boolean running = true;
  // userId -> clientHandler
  private static final Map<Integer, ClientHandler> authenticatedClients = new ConcurrentHashMap<>();
  private static final ExecutorService clientPool = Executors.newCachedThreadPool();
  private static final ExecutorService broadcastPool = Executors.newCachedThreadPool();
  private AuctionService auctionService;
  private BidService bidService;
  private UserService userService;
  private ItemService itemService;

  private Server() {
    try {
      serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
      serverSocket.bind(new InetSocketAddress(PORT));
      logger.info("[SERVER] Running on port {}", PORT);
      initService();
    } catch (IOException e) {
      logger.error("[SERVER] Failed to start on port {}", PORT, e);
      throw new RuntimeException("Cannot start server", e);
    }
  }

  public static synchronized Server getInstance() {
    if (instance == null) {
      instance = new Server();
    }
    return instance;
  }

  private void initService() {
    logger.info("[SERVER] Initializing database...");
    // DAO
    UserDAO userDAO = new MySqlUserDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    // SERVICE
    // cái factory này để code khởi tạo ở đây trông ngắn đi, tác dụng chính là tạo 1 đổi tượng
    // TransactionManager dùng chung
    ServiceFactory serviceFactory = new ServiceFactory();
    userService = new UserService(userDAO);
    itemService = serviceFactory.createItemService(itemDAO);
    bidService = serviceFactory.createBidService(bidDAO, auctionDAO);
    auctionService = serviceFactory.createAuctionService(auctionDAO, bidDAO, itemDAO);
  }

  public void start() {
    logger.info("[SERVER] Waiting for clients...");
    try {
      while (running && !serverSocket.isClosed() && !Thread.currentThread().isInterrupted()) {
        try {
          Socket socket = serverSocket.accept();
          if (!running) {
            socket.close();
            break;
          }
          socket.setTcpNoDelay(true);
          socket.setKeepAlive(true);
          socket.setSoTimeout(30000);
          logger.info("[SERVER] Client connected: {}", socket.getRemoteSocketAddress());
          ClientHandler clientHandler =
              new ClientHandler(socket, auctionService, bidService, userService, itemService);
          clientPool.execute(clientHandler);
        } catch (SocketException e) {
          if (serverSocket.isClosed()) {
            logger.info("[SERVER] Server socket closed");
            break;
          }
          logger.error("[SERVER] Socket error", e);
        }
      }
    } catch (IOException e) {
      if (running) {
        logger.error("[SERVER] Error accepting client", e);
      }
    }
  }

  public void shutdown() {
    logger.info("[SERVER] Shutting down...");
    running = false;
    try {
      // close server socket
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
      // close clients safely
      authenticatedClients.values().stream().toList().forEach(ClientHandler::close);
      authenticatedClients.clear();
      // shutdown client pool
      shutdownExecutor(clientPool, "clientPool");
      // shutdown broadcast pool
      shutdownExecutor(broadcastPool, "broadcastPool");
      logger.info("[SERVER] Shutdown complete");
      instance = null;
    } catch (IOException e) {
      logger.error("[SERVER] Shutdown IO error", e);
    }
  }

  private void shutdownExecutor(ExecutorService executor, String name) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        logger.warn("[SERVER] Force shutdown {}", name);
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("[SERVER] Shutdown interrupted: {}", name, e);
      executor.shutdownNow();
    }
  }

  public static void registerClient(int userId, ClientHandler handler) {
    ClientHandler old = authenticatedClients.put(userId, handler);
    if (old != null && old != handler) {
      logger.info("[SERVER] Replacing old auction for user {}", userId);
      authenticatedClients.remove(userId, old);
      old.close();
    }
  }

  public static void removeClient(int userId, ClientHandler handler) {
    authenticatedClients.remove(userId, handler);
  }

  public static void broadcast(PacketRes packet, int excludeUser) {
    if (packet == null) {
      return;
    }
    authenticatedClients
        .values()
        .forEach(
            handler -> {
              if (handler == null || !handler.isAuthenticated()) {
                return;
              }
              var user = handler.getUser();
              if (user == null) {
                return;
              }
              if (user.getId() == excludeUser) {
                return;
              }
              broadcastPool.execute(
                  () -> {
                    try {
                      handler.sendPacket(packet);
                    } catch (Exception e) {
                      logger.warn("[SERVER] Broadcast failed to user {}", user.getId(), e);
                    }
                  });
            });
  }

  public static int getOnlineUserCount() {
    return authenticatedClients.size();
  }

  public static boolean isUserOnline(int userId) {
    return authenticatedClients.containsKey(userId);
  }
}
