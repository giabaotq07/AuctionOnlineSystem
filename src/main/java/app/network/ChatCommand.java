package app.network;

import app.enums.PacketType;
import app.models.Packet;

public class ChatCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    String content = (String) packet.getData();
    Packet chatPacket = new Packet(PacketType.CHAT, content);
    Server.broadcast(chatPacket);
  }
}
