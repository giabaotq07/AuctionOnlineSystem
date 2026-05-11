package app.network;

import app.data.ChatRequest;
import app.data.ChatResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;

public class ChatCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    ChatRequest chatRequest = packet.getData(ChatRequest.class);
    ChatResponse chatResponse =
        new ChatResponse(
            chatRequest.sender().name(), chatRequest.content(), chatRequest.timestamp());
    PacketRes chatPacket = PacketRes.of(PacketType.CHAT, chatResponse);
    Server.broadcast(chatPacket, chatRequest.sender().id());
  }
}
