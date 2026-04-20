package app.network;

import app.models.MessagePacket;
import app.models.CommandType;
import app.network.services.AuctionService;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
  private final Socket socket;
  private BufferedReader reader;
  private PrintWriter writer;
  private Integer userId;
  private final Gson gson = new Gson();

  public ClientHandler(Socket socket) { this.socket = socket; }

  @Override
  public void run() {
    try {
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer = new PrintWriter(socket.getOutputStream(), true);
      String line;
      while ((line = reader.readLine()) != null) {
        MessagePacket<?> packet = gson.fromJson(line, MessagePacket.class);
        handlePacket(packet);
      }
    } catch (IOException e) { close(); }
  }

  private void handlePacket(MessagePacket<?> packet) {
    if (packet.getType() == CommandType.LOGIN) {
      this.userId = 1; // Giả lập lấy ID từ DB thành công
      Server.registerClient(this.userId, this);
    } else if (packet.getType() == CommandType.PLACE_BID) {
      double amount = (double) packet.getData();
      if (AuctionService.getInstance().placeBid(1, this.userId, amount)) {
        Server.broadcast(new MessagePacket<>(CommandType.UPDATE_PRICE, amount));
      } else {
        sendMessage(MessagePacket.error("Giá đặt không hợp lệ!"));
      }
    }
  }

  public void sendMessage(MessagePacket<?> packet) {
    if (writer != null) writer.println(gson.toJson(packet));
  }

  private void close() {
    if (userId != null) Server.removeClient(userId);
    try { socket.close(); } catch (IOException e) {}
  }
}