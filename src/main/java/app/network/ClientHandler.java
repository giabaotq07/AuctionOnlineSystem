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
  private String username;
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
    switch (packet.getType()) {
      case LOGIN:
        // Giả sử data gửi lên là chuỗi Username
        this.username = (String) packet.getData();
        Server.registerClient(this.username, this);

        System.out.println("[SERVER] Người dùng " + this.username + " đã kết nối.");
        sendMessage(new MessagePacket<>(CommandType.SUCCESS, "Chào " + this.username));
        break;

      case CHAT:
        String content = (String) packet.getData();
        // Gửi kèm Username người gửi để mọi người biết ai đang chat
        MessagePacket<String> chatPacket = new MessagePacket<>(CommandType.CHAT, content);
        // Bạn có thể tùy biến: MessagePacket<>(CommandType.CHAT, this.username + ": " + content);
        Server.broadcast(chatPacket);
        break;

      case PLACE_BID:
      double amount = (double) packet.getData();
      if (AuctionService.getInstance().placeBid(1, this.username, amount)) {
        Server.broadcast(new MessagePacket<>(CommandType.UPDATE_PRICE, amount));
      } else {
        sendMessage(MessagePacket.error("Giá đặt không hợp lệ!"));
      }
      break;
    }
  }

  public void sendMessage(MessagePacket<?> packet) {
    if (writer != null) writer.println(gson.toJson(packet));
  }

  private void close() {
    if (this.username != null) {
      Server.removeClient(this.username);
    }
    try { socket.close(); } catch (IOException e) {}
  }
}