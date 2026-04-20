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
        this.username = String.valueOf(packet.getData());
        Server.registerClient(this.username, this);
        System.out.println("[SERVER] " + this.username + " đã đăng nhập.");
        // Phản hồi thành công
        MessagePacket<String> welcome = new MessagePacket<>(CommandType.SUCCESS, "Chào mừng!");
        welcome.setMessage("Hệ thống");
        sendMessage(welcome);
        break;

      case CHAT:
        String content = (String) packet.getData();
        MessagePacket<String> chatPacket = new MessagePacket<>(CommandType.CHAT, content);
        // QUAN TRỌNG: Gán tên người gửi vào đây
        chatPacket.setMessage(this.username);
        Server.broadcast(chatPacket);
        break;

      case PLACE_BID:
        // Tương tự cho đấu giá
        double amount = Double.parseDouble(packet.getData().toString());
        if (AuctionService.getInstance().placeBid(1, this.username, amount)) {
          MessagePacket<String> bidPacket = new MessagePacket<>(CommandType.UPDATE_PRICE, String.valueOf(amount));
          bidPacket.setMessage(this.username); // Ai là người trả giá cao nhất
          Server.broadcast(bidPacket);
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