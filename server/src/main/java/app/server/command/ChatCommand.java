package app.server.command;

import app.common.dto.ChatRequest;
import app.common.dto.ChatResponse;
import app.common.enums.ResponseType;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** ChatCommand. */
public class ChatCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(ChatCommand.class);
  private static final int MAX_MESSAGE_LENGTH = 500;

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        clientHandler.sendPacket(
            PacketRes.error(ResponseType.CHAT_RESULT, "Authentication required"));
        return;
      }
      ChatRequest request = packet.getData(ChatRequest.class);
      if (request == null) {
        clientHandler.sendPacket(PacketRes.error(ResponseType.CHAT_RESULT, "Invalid request"));
        return;
      }
      String content = request.content();
      if (content == null || content.isBlank()) {
        clientHandler.sendPacket(
            PacketRes.error(ResponseType.CHAT_RESULT, "Message cannot be empty"));
        return;
      }
      content = content.trim();
      if (content.length() > MAX_MESSAGE_LENGTH) {
        clientHandler.sendPacket(PacketRes.error(ResponseType.CHAT_RESULT, "Message too long"));
        return;
      }
      User user = clientHandler.getUser();
      // KHÔNG trust sender từ client
      ChatResponse response =
          new ChatResponse(user.getId(), user.getName(), content, LocalDateTime.now());
      PacketRes chatPacket = PacketRes.of(ResponseType.CHAT_MESSAGE, "OK", response);
      clientHandler.sendPacket(chatPacket);
      Server.broadcast(chatPacket, user.getId());
      logger.info("User {} sent chat message", user.getId());
    } catch (Exception e) {
      logger.error("Chat network failed", e);
      clientHandler.sendPacket(PacketRes.error(ResponseType.CHAT_RESULT, "Failed to send message"));
    }
  }
}
