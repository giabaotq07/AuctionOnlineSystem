package app.network;

import app.enums.PacketType;
import app.exception.AppException;
import app.models.Packet;
import app.models.User;
import app.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
  private final Socket socket;
  private BufferedWriter writer;
  private BufferedReader reader;
  private User user;
  private String username;

  private static final Map<PacketType, Command> COMMANDS = new HashMap<>();

  static {
    COMMANDS.put(PacketType.LOGIN, new LoginCommand());
    COMMANDS.put(PacketType.CHAT, new ChatCommand());
    COMMANDS.put(PacketType.PLACE_BID, new PlaceBidCommand());
    COMMANDS.put(PacketType.REGISTER, new RegisterCommand());
    COMMANDS.put(PacketType.CREATE_AUCTION, new CreateAuctionCommand());
    COMMANDS.put(PacketType.FETCH_AUCTIONS, new FetchAuctionsCommand());
    COMMANDS.put(PacketType.FETCH_HISTORY, new FetchHistoryCommand());
    COMMANDS.put(PacketType.FETCH_AUCTION_DETAIL, new FetchAuctionDetailCommand());
    COMMANDS.put(PacketType.FETCH_AUCTION_RESULT, new FetchAuctionResultCommand());
  }

  public ClientHandler(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      writer.flush();
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      listen();
    } catch (IOException e) {
      logger.error("Error initializing client handler", e);
      close();
    }
  }

  public void listen() {
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        try {
          Packet packet = JsonUtil.fromJson(line, Packet.class);
          handlePacket(packet);
        } catch (AppException e) {
          // Lỗi JSON: Log lại và tiếp tục nghe gói tiếp theo
          logger.error("Gói tin không hợp lệ: {}", e.getMessage());
        }
      }
    } catch (SocketException e) {
      // Hứng lỗi "Connection reset" ở đây
      logger.info("Client {} đã ngắt kết nối đột ngột (Connection reset).", username);
    } catch (IOException e) {
      logger.error("Lỗi I/O khi lắng nghe Client: {}", e.getMessage());
    } finally {
      close(); // Đảm bảo dọn dẹp tài nguyên và xóa khỏi danh sách Server
    }
  }

  private void handlePacket(Packet packet) {
    Command command = COMMANDS.get(packet.getType());
    if (command != null) {
      logger.info("[Server] Processing command: {}", packet.getType());
      command.execute(this, packet);
    } else {
      logger.warn("[SERVER] Unrecognized command type: {}", packet.getType());
    }
  }

  public void sendMessage(Packet packet) {
    if (writer != null) {
      try {
        writer.write(JsonUtil.toJson(packet));
        writer.newLine();
        writer.flush();
        logger.debug("Sent message to {}", username);
      } catch (IOException e) {
        logger.error("[SERVER] Failed to send message to {}: {}", username, e.getMessage());
      }
    }
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
    this.username = user.getName();
  }

  private void close() {
    if (this.username != null) {
      Server.removeClient(this.user.getId());
      logger.info("Client {} disconnected", username);
    }
    try {
      socket.close();
    } catch (IOException ignored) {
    }
  }
}
