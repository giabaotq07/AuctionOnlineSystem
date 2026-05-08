package app.network;

import app.dto.ChatRequest;
import app.dto.ChatResponse;
import app.enums.PacketType;
import app.models.Packet;
import app.utils.JsonUtil;

public class ChatCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Packet packet) {
    ChatRequest chatRequest = JsonUtil.fromJson(packet.getData(), ChatRequest.class);
    ChatResponse chatResponse =
        new ChatResponse(
            chatRequest.sender().getName(), chatRequest.content(), chatRequest.timestamp());
    Packet chatPacket = new Packet(PacketType.CHAT, JsonUtil.toJsonElement(chatResponse));
    Server.broadcast(chatPacket, chatRequest.sender().getId());
  }
}
