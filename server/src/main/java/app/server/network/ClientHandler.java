package app.server.network;

import app.common.enums.RequestType;
import app.common.enums.ResponseType;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.common.utils.JsonUtil;
import app.server.command.*;
import app.server.service.AuctionService;
import app.server.service.BidService;
import app.server.service.ItemService;
import app.server.service.UserService;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** ClientHandler. */
public class ClientHandler implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

  private final Socket socket;
  private BufferedWriter writer;
  private BufferedReader reader;
  private final Object writeLock = new Object();
  private final Session session = new Session();
  private final Map<RequestType, Command> commands;
  private volatile boolean closed = false;

  /** ClientHandler. */
  public ClientHandler(
      Socket socket,
      AuctionService auctionService,
      BidService bidService,
      UserService userService,
      ItemService itemService) {
    this.socket = socket;
    this.commands = createCommands(auctionService, bidService, userService, itemService);
  }

  private Map<RequestType, Command> createCommands(
      AuctionService auctionService,
      BidService bidService,
      UserService userService,
      ItemService itemService) {
    Map<RequestType, Command> registry = new EnumMap<>(RequestType.class);
    registry.put(RequestType.CHAT, new ChatCommand());
    registry.put(RequestType.LOGIN, new LoginCommand(userService));
    registry.put(RequestType.REGISTER, new RegisterCommand(userService));
    registry.put(RequestType.CREATE_AUCTION, new CreateAuctionCommand(auctionService));
    registry.put(RequestType.UPDATE_AUCTION, new UpdateAuctionCommand(auctionService));
    registry.put(
        RequestType.FETCH_AUCTION_SUMMARIES, new FetchAuctionSummariesCommand(auctionService));
    registry.put(RequestType.FETCH_AUCTION_HISTORY, new FetchAuctionHistoryCommand(auctionService));
    registry.put(RequestType.FETCH_AUCTION_DETAIL, new FetchAuctionDetailCommand(auctionService));
    registry.put(RequestType.UNWATCH_AUCTION, new UnwatchAuctionCommand());
    registry.put(
        RequestType.FETCH_AUCTION_RESULT,
        new FetchAuctionResultCommand(auctionService, userService));
    registry.put(RequestType.FETCH_SELLER_ITEMS, new FetchSellerItemsCommand(itemService));
    registry.put(RequestType.FETCH_USER_LIST, new FetchUserListCommand(userService));
    registry.put(RequestType.CANCEL_AUCTION, new CancelAuctionCommand(auctionService, userService));
    registry.put(
        RequestType.PLACE_BID, new PlaceBidCommand(bidService, userService, auctionService));
    registry.put(RequestType.DEPOSIT, new DepositCommand(userService));
    registry.put(RequestType.SETTLE_WALLET, new SettleWalletCommand(auctionService, userService));
    registry.put(
        PacketType.UPLOAD_IMAGE,
        new UploadImageCommand(itemService, imageStorageService, auctionService));
    registry.put(PacketType.FETCH_ITEM_IMAGE, new FetchItemImageCommand(imageStorageService));

    return registry;
  }

  @Override
  public void run() {
    try {
      socket.setSoTimeout(0);
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      listen();
    } catch (IOException e) {
      logger.error("Error initializing client command", e);
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
            sendPacket(PacketRes.error(ResponseType.ERROR, "Packet type is required"));
            continue;
          }
          session.touch();
          handlePacket(packet);
        } catch (Exception e) {
          logger.error("Invalid packet received", e);
          sendPacket(PacketRes.error(ResponseType.ERROR, "Invalid packet received"));
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
    RequestType type = packet.getType();
    logger.info("Processing network: {}", type);
    if (!authorize(type)) {
      return;
    }
    Command command = commands.get(type);
    if (command == null) {
      logger.warn("Unrecognized network type: {}", type);
      sendPacket(PacketRes.error(ResponseType.ERROR, "Unrecognized network type"));
      return;
    }
    try {
      command.execute(this, packet);
    } catch (Exception e) {
      logger.error("Error executing network: {}", type, e);
      sendPacket(PacketRes.error(toResponseType(type), "Error executing network"));
    }
  }

  private ResponseType toResponseType(RequestType type) {
    return switch (type) {
      case LOGIN -> ResponseType.LOGIN_RESULT;
      case REGISTER -> ResponseType.REGISTER_RESULT;
      case CREATE_AUCTION -> ResponseType.CREATE_AUCTION_RESULT;
      case UPDATE_AUCTION -> ResponseType.UPDATE_AUCTION_RESULT;
      case FETCH_AUCTION_SUMMARIES -> ResponseType.FETCH_AUCTION_SUMMARIES_RESULT;
      case FETCH_AUCTION_HISTORY -> ResponseType.FETCH_AUCTION_HISTORY_RESULT;
      case FETCH_AUCTION_DETAIL -> ResponseType.FETCH_AUCTION_DETAIL_RESULT;
      case FETCH_AUCTION_RESULT -> ResponseType.AUCTION_RESULT_FETCHED;
      case FETCH_SELLER_ITEMS -> ResponseType.FETCH_SELLER_ITEMS_RESULT;
      case FETCH_USER_LIST -> ResponseType.FETCH_USER_LIST_RESULT;
      case CANCEL_AUCTION -> ResponseType.CANCEL_AUCTION_RESULT;
      case PLACE_BID -> ResponseType.PLACE_BID_RESULT;
      case DEPOSIT -> ResponseType.DEPOSIT_RESULT;
      case SETTLE_WALLET -> ResponseType.SETTLE_WALLET_RESULT;
      case CHAT -> ResponseType.CHAT_RESULT;
      case UPLOAD_IMAGE -> ResponseType.UPLOAD_IMAGE;
      case FETCH_ITEM_IMAGE -> ResponseType.FETCH_ITEM_IMAGE;
      default -> ResponseType.ERROR;
    };
  }

  private boolean authorize(RequestType type) {
    if (type.requiresAuthentication() && !session.isAuthenticated()) {
      sendPacket(PacketRes.error(toResponseType(type), "Authentication required"));
      return false;
    }
    User user = session.getUser();
    if (!type.isAllowed(user == null ? null : user.getRole())) {
      sendPacket(
          PacketRes.error(toResponseType(type), "Bạn không có quyền thực hiện yêu cầu này."));
      return false;
    }
    return true;
  }

  /** sendPacket. */
  public void sendPacket(PacketRes packet) {
    send(packet);
  }

  private void send(Object packet) {
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

  /** close. */
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
      // Socket may already be half-closed by the peer.
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

  public Session getSession() {
    return session;
  }

  public User getUser() {
    return session.getUser();
  }

  public Socket getSocket() {
    return socket;
  }
}
