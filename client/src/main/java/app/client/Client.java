package app.client;

import app.client.command.*;
import app.common.enums.ResponseType;
import app.common.exception.ConnectException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.common.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.util.EnumMap;
import java.util.Map;
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
  private boolean paidNoticeRegistered = false;

  private final Map<ResponseType, Command> commands;

  private Client() {
    this.commands = createCommands();
  }

  private Map<ResponseType, Command> createCommands() {
    Map<ResponseType, Command> registry = new EnumMap<>(ResponseType.class);
    registry.put(ResponseType.CHAT_RESULT, new ChatCommand());
    registry.put(ResponseType.LOGIN_RESULT, new LoginCommand());
    registry.put(ResponseType.REGISTER_RESULT, new RegisterCommand());
    registry.put(ResponseType.CREATE_AUCTION_RESULT, new CreateAuctionCommand());
    registry.put(ResponseType.UPDATE_AUCTION_RESULT, new UpdateAuctionCommand());
    registry.put(ResponseType.FETCH_AUCTION_SUMMARIES_RESULT, new FetchAuctionSummariesCommand());
    registry.put(ResponseType.FETCH_AUCTION_HISTORY_RESULT, new FetchAuctionHistoryCommand());
    registry.put(ResponseType.FETCH_AUCTION_DETAIL_RESULT, new FetchAuctionDetailCommand());
    registry.put(ResponseType.AUCTION_RESULT_FETCHED, new FetchAuctionResultCommand());
    registry.put(ResponseType.CANCEL_AUCTION_RESULT, new CancelAuctionCommand());
    registry.put(ResponseType.PLACE_BID_RESULT, new PlaceBidCommand());
    registry.put(ResponseType.DEPOSIT_RESULT, new DepositCommand());
    registry.put(ResponseType.CHAT_MESSAGE, new ChatCommand());
    registry.put(ResponseType.AUCTION_CREATED, new CreateAuctionCommand());
    registry.put(ResponseType.BID_PLACED, new PlaceBidCommand());
    registry.put(ResponseType.AUCTION_CANCELLED, new CancelAuctionCommand());
    registry.put(ResponseType.AUCTION_SUMMARIES_UPDATED, new FetchAuctionSummariesCommand());
    registry.put(ResponseType.AUCTION_DETAIL_UPDATED, new FetchAuctionDetailCommand());
    registry.put(ResponseType.WALLET_UPDATED, new WalletUpdateCommand());
    registry.put(ResponseType.SETTLE_WALLET_RESULT, new WalletUpdateCommand());
    return registry;
  }

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
    //    registerPaidNoticeListener();
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
        handleIncoming(line);
      }
    } catch (IOException e) {
      if (!closed) {
        logger.warn("[CLIENT] Socket closed");
      }
    } finally {
      closeResources();
    }
  }

  private void handleIncoming(String line) {
    try {
      PacketRes packet = JsonUtil.fromJson(line, PacketRes.class);
      if (packet == null || packet.getType() == null) {
        logger.error("[CLIENT] Packet type is required");
        return;
      }
      handlePacket(packet);
    } catch (Exception e) {
      logger.error("[CLIENT] Invalid packet received", e);
    }
  }

  private void handlePacket(PacketRes packet) {
    ResponseType type = packet.getType();
    logger.info("Processing packet: {}", type);
    Command command = commands.get(type);
    if (command == null) {
      logger.warn("Unrecognized packet type: {}", type);
      return;
    }
    try {
      command.execute(packet);
    } catch (Exception e) {
      logger.error("Error executing packet: {}", type, e);
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

  /** closeResources. */
  public synchronized void closeResources() {
    if (closed) {
      return;
    }
    logger.info("[CLIENT] Closing resources");
    closed = true;
    connected = false;
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

  //  private void registerPaidNoticeListener() {
  //    if (paidNoticeRegistered) {
  //      return;
  //    }
  //    paidNoticeRegistered = true;
  //    ClientNotificationCenter.getInstance().addUpdateListener(
  //        ResponseType.AUCTION_PAID_NOTICE,
  //        AuctionPaidNoticeResponse.class,
  //        (response, success, message) -> {
  //          if (!success || response == null) {
  //            return;
  //          }
  //          String roleLabel = "WINNER".equalsIgnoreCase(response.role())
  //              ? "Bạn đã thanh toán"
  //              : "Bạn đã nhận thanh toán";
  //          DecimalFormat formatter = new DecimalFormat("#,###");
  //          String amountText = response.amount() == null
  //              ? "0"
  //              : formatter.format(response.amount());
  //          String content =
  //              roleLabel
  //                  + "\nPhiên: "
  //                  + response.auctionName()
  //                  + "\nSố tiền: "
  //                  + amountText
  //                  + " đ";
  //          Platform.runLater(() -> AlertUtils.showInfo("Thanh toán thành công", content));
  //        });
  //  }
}
