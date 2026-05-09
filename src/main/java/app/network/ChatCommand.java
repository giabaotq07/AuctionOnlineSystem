package app.network;

import app.data.ChatRequest;
import app.data.ChatResponse;
import app.enums.PacketType;
import app.models.Packet;
import app.utils.JsonUtil;

public class ChatCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    ChatRequest chatRequest = JsonUtil.fromJson(packet.getData(), ChatRequest.class);
    ChatResponse chatResponse =
        new ChatResponse(
            chatRequest.sender().name(), chatRequest.content(), chatRequest.timestamp());
    Packet chatPacket = new Packet(PacketType.CHAT, JsonUtil.toJson(chatResponse));
    Server.broadcast(chatPacket, chatRequest.sender().id());
  }
}
