package app.network;

import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.Session;
import app.models.User;
import app.service.*;
import app.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
  private static final Map<PacketType, Command> commands = new HashMap<>();

  static {
    commands.put(PacketType.CHAT, new ChatCommand());
  }

  private final Socket socket;
  private BufferedWriter writer;
  private BufferedReader reader;
  private final Object writeLock = new Object();
  private final Session session = new Session();
  private volatile boolean closed = false;
  private final AuctionService auctionService;
  private final BidService bidService;
  private final UserService userService;
  private final ItemService itemService;

  public ClientHandler(
      Socket socket,
      AuctionService auctionService,
      BidService bidService,
      UserService userService,
      ItemService itemService) {
    this.socket = socket;
    this.auctionService = auctionService;
    this.bidService = bidService;
    this.userService = userService;
    this.itemService = itemService;
    initCommands();
  }

  private void initCommands() {
    commands.putIfAbsent(PacketType.LOGIN, new LoginCommand(userService));
    commands.putIfAbsent(PacketType.REGISTER, new RegisterCommand(userService));
    commands.putIfAbsent(
        PacketType.CREATE_AUCTION, new CreateAuctionCommand(auctionService, itemService));
    commands.putIfAbsent(PacketType.FETCH_AUCTIONS, new FetchAuctionsCommand(auctionService));
    commands.putIfAbsent(PacketType.FETCH_HISTORY, new FetchHistoryCommand(auctionService));
    commands.putIfAbsent(
        PacketType.FETCH_AUCTION_DETAIL, new FetchAuctionDetailCommand(auctionService));
    commands.putIfAbsent(
        PacketType.FETCH_AUCTION_RESULT, new FetchAuctionResultCommand(auctionService));
    commands.putIfAbsent(PacketType.PLACE_BID, new PlaceBidCommand(bidService, userService));
    commands.putIfAbsent(PacketType.DEPOSIT, new DepositCommand(userService));
    commands.putIfAbsent(
        PacketType.SETTLE_WALLET, new SettleWalletCommand(auctionService, userService));
  }

  @Override
  public void run() {
    try {
      socket.setSoTimeout(0);
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      listen();
    } catch (IOException e) {
      logger.error("Error initializing client handler", e);
    } finally {
      cleanup();
    }
  }

  private void listen() {
    try {
      String line;
      while (!closed && (line = reader.readLine()) != null) {
        try {
          PacketReq packet = JsonUtil.fromJson(line, PacketReq.class);
          if (packet == null || packet.getType() == null) {
            sendPacket(PacketRes.of(false, PacketType.ERROR, "Packet type is required"));
            continue;
          }
          session.touch();
          handlePacket(packet);
        } catch (Exception e) {
          logger.error("Invalid packet received", e);
          sendPacket(PacketRes.error(PacketType.ERROR, "Invalid packet received"));
        }
      }
      logger.info("Client disconnected: {}", socket.getRemoteSocketAddress());
    } catch (SocketTimeoutException e) {
      logger.warn("Client timeout: {}", socket.getRemoteSocketAddress());
    } catch (IOException e) {
      if (!closed) {
        logger.error("Error while listening to client", e);
      }
    }
  }

  private void handlePacket(PacketReq packet) {
    PacketType type = packet.getType();
    logger.info("Processing command: {}", type);
    if (requiresAuthentication(type) && !requireLogin()) {
      return;
    }
    Command command = commands.get(type);
    if (command == null) {
      logger.warn("Unrecognized command type: {}", type);
      sendPacket(PacketRes.error(PacketType.ERROR, "Unrecognized command type"));
      return;
    }
    try {
      command.execute(this, packet);
    } catch (Exception e) {
      logger.error("Error executing command: {}", type, e);
      sendPacket(PacketRes.error(PacketType.ERROR, "Error executing command"));
    }
  }

  private boolean requiresAuthentication(PacketType type) {
    return switch (type) {
      case PLACE_BID, CREATE_AUCTION, FETCH_HISTORY, DEPOSIT, SETTLE_WALLET -> true;
      default -> false;
    };
  }

  private boolean requireLogin() {
    if (!session.isAuthenticated()) {
      sendPacket(PacketRes.error(PacketType.ERROR, "Authentication required"));
      return false;
    }
    return true;
  }

  public void sendPacket(PacketRes packet) {
    if (packet == null || writer == null || closed) {
      return;
    }
    try {
      synchronized (writeLock) {
        writer.write(JsonUtil.toJson(packet));
        writer.newLine();
        writer.flush();
      }
      User user = session.getUser();
      logger.debug("Sent message to {}", user != null ? user.getName() : "unknown");
    } catch (IOException e) {
      logger.error("Failed to send message", e);
      close();
    }
  }

  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      if (socket != null && !socket.isClosed()) {
        socket.shutdownInput();
        socket.shutdownOutput();
      }
    } catch (IOException ignored) {
    }
    try {
      if (reader != null) {
        reader.close();
      }
    } catch (IOException e) {
      logger.error("Error closing reader", e);
    }
    try {
      if (writer != null) {
        writer.close();
      }
    } catch (IOException e) {
      logger.error("Error closing writer", e);
    }
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      logger.error("Error closing socket", e);
    }
  }

  private void cleanup() {
    User user = session.getUser();
    if (user != null) {
      Server.removeClient(user.getId(), this);
    }
    session.logout();
    close();
  }

  public boolean isAuthenticated() {
    return session.isAuthenticated();
  }

  public Session getAuction() {
    return session;
  }

  public User getUser() {
    return session.getUser();
  }

  public Socket getSocket() {
    return socket;
  }

  public BufferedWriter getWriter() {
    return writer;
  }

  public BufferedReader getReader() {
    return reader;
  }

  public AuctionService getAuctionService() {
    return auctionService;
  }

  public BidService getBidService() {
    return bidService;
  }

  public UserService getUserService() {
    return userService;
  }

  public ItemService getItemService() {
    return itemService;
  }
}
