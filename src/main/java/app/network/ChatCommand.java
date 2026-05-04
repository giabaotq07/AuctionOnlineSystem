package app.network;

import app.enums.Result;
import app.models.ResponsePacket;

public class ChatCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, ResponsePacket<?> packet) {
    String content = (String) packet.getData();
    ResponsePacket<String> chatPacket = new ResponsePacket<>(Result.CHAT, content);
    chatPacket.setMessage(clientHandler.getUsername());
    Server.broadcast(chatPacket);
  }
}
