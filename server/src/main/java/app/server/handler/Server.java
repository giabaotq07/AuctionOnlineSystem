package app.server.handler;

import app.common.dto.AuctionSummariesResponse;
import app.common.enums.PacketType;
import app.common.mapper.DtoMapper;
import app.common.models.PacketRes;
import app.server.dao.*;
import app.server.dao.impl.*;
import app.server.database.TransactionManager;
import app.server.service.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server. */
public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);
  public static final int PORT = 5000;
  private static volatile Server instance;
  private ServerSocket serverSocket;
  private volatile boolean running = true;
  private static final Map<Integer, app.server.handler.ClientHandler> authenticatedClients =
      new ConcurrentHashMap<>();
  private static final ExecutorService clientPool = Executors.newCachedThreadPool();
  private static final ExecutorService broadcastPool = Executors.newCachedThreadPool();
  private final ScheduledExecutorService auctionMaintenancePool =
      Executors.newSingleThreadScheduledExecutor();
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

  /** getInstance. */
  public static synchronized Server getInstance() {
    if (instance == null) {
      instance = new Server();
    }
    return instance;
  }

  private void initService() {
    logger.info("[SERVER] Initializing database...");
    UserDAO userDAO = new MySqlUserDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    TransactionManager transactionManager = new TransactionManager();
    BidValidator bidValidator = new BidValidator();
    AntiSnipeService antiSnipeService = new AntiSnipeService();
    userService = new UserService(userDAO, transactionManager);
    itemService = new ItemService(itemDAO, auctionDAO, transactionManager);
    bidService =
        new BidService(
            bidDAO, auctionDAO, userDAO, transactionManager, bidValidator, antiSnipeService);
    auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO, userDAO, transactionManager);
    AuctionScheduler.getInstance().init(auctionService, userService);
    startAuctionMaintenance();
  }

  private void startAuctionMaintenance() {
    auctionMaintenancePool.scheduleAtFixedRate(
        () -> {
          try {
            var completedIds = auctionService.completeExpiredAuctions();
            if (!completedIds.isEmpty()) {
              for (Integer auctionId : completedIds) {
                AuctionScheduler.getInstance().notifyPaymentIfNeeded(auctionId);
              }
              logger.info("[SERVER] Completed expired auctions: {}", completedIds);
              broadcastAuctionList();
            }
          } catch (Exception e) {
            logger.error("[SERVER] Auction maintenance failed", e);
          }
        },
        5,
        5,
        TimeUnit.SECONDS);
  }

  private void broadcastAuctionList() {
    try {
      var response =
          new AuctionSummariesResponse(
              auctionService.getAuctions().stream()
                  .map(snapshot -> DtoMapper.toAuctionSummary(snapshot.auction(), snapshot.item()))
                  .toList());
      broadcast(PacketRes.of(PacketType.FETCH_AUCTION_SUMMARIES, response), -1);
    } catch (Exception e) {
      logger.error("[SERVER] Failed to broadcast auction list", e);
    }
  }

  /** start. */
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
          socket.setSoTimeout(0);
          logger.info("[SERVER] Client connected: {}", socket.getRemoteSocketAddress());
          app.server.handler.ClientHandler clientHandler =
              new app.server.handler.ClientHandler(
                  socket, auctionService, bidService, userService, itemService);
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

  /** shutdown. */
  public void shutdown() {
    logger.info("[SERVER] Shutting down...");
    running = false;
    AuctionScheduler.getInstance().shutdown();
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
      authenticatedClients.values().stream()
          .toList()
          .forEach(app.server.handler.ClientHandler::close);
      authenticatedClients.clear();
      shutdownExecutor(clientPool, "clientPool");
      shutdownExecutor(broadcastPool, "broadcastPool");
      shutdownExecutor(auctionMaintenancePool, "auctionMaintenancePool");
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

  /** registerClient. */
  public static void registerClient(int userId, app.server.handler.ClientHandler handler) {
    app.server.handler.ClientHandler old = authenticatedClients.put(userId, handler);
    if (old != null && old != handler) {
      logger.info("[SERVER] Replacing old auction for user {}", userId);
      authenticatedClients.remove(userId, old);
      old.close();
    }
  }

  /** removeClient. */
  public static void removeClient(int userId, ClientHandler handler) {
    authenticatedClients.remove(userId, handler);
  }

  /** sendToUser. */
  public static void sendToUser(int userId, PacketRes packet) {
    if (packet == null) {
      return;
    }
    ClientHandler handler = authenticatedClients.get(userId);
    if (handler == null || !handler.isAuthenticated()) {
      return;
    }
    broadcastPool.execute(
        () -> {
          try {
            handler.sendPacket(packet);
          } catch (Exception e) {
            logger.warn("[SERVER] Send failed to user {}", userId, e);
          }
        });
  }

  /** broadcast. */
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

  /** isUserOnline. */
  public static boolean isUserOnline(int userId) {
    return authenticatedClients.containsKey(userId);
  }
}
