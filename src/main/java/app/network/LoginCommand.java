package app.network;

import app.enums.CommandType;
import app.models.MessagePacket;

public class LoginCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, MessagePacket<?> packet) {
    String username = String.valueOf(packet.getData());
    clientHandler.setUsername(username);
    Server.registerClient(username, clientHandler);
    System.out.println("[SERVER] " + username + " đã đăng nhập.");
    MessagePacket<String> welcome = new MessagePacket<>(CommandType.SUCCESS, "Chào mừng!");
    welcome.setMessage("Hệ thống");
    clientHandler.sendMessage(welcome);
  }
}
