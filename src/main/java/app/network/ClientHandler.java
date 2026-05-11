package app.network;

import app.enums.PacketType;
import app.exception.AppException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
  private final Socket socket;
  private BufferedWriter writer;
  private BufferedReader reader;
  private User user;
  private String username;

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
          PacketReq packet = JsonUtil.fromJson(line, PacketReq.class);
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

  private void handlePacket(PacketReq packet) {
    PacketType type = packet.getType();
    if (type == null) {
      logger.warn("[SERVER] Unrecognized command type: null");
      return;
    }
    logger.info("[Server] Processing command: {}", type);
    Command command;
    switch (type) {
      case LOGIN:
        command = new LoginCommand();
        break;
      case CHAT:
        command = new ChatCommand();
        break;
      case PLACE_BID:
        command = new PlaceBidCommand();
        break;
      case REGISTER:
        command = new RegisterCommand();
        break;
      case CREATE_AUCTION:
        command = new CreateAuctionCommand();
        break;
      case FETCH_AUCTIONS:
        command = new FetchAuctionsCommand();
        break;
      case FETCH_HISTORY:
        command = new FetchHistoryCommand();
        break;
      case FETCH_AUCTION_DETAIL:
        command = new FetchAuctionDetailCommand();
        break;
      case FETCH_AUCTION_RESULT:
        command = new FetchAuctionResultCommand();
        break;
      default:
        logger.warn("[SERVER] Unrecognized command type: {}", type);
        return;
    }
    command.execute(this, packet);
  }

  public void sendMessage(PacketRes packet) {
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
