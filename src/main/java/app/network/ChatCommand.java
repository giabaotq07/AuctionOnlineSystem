package app.network;

import app.data.ChatRequest;
import app.data.ChatResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(ChatCommand.class);
  private static final int MAX_MESSAGE_LENGTH = 500;

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Authentication required"));
        return;
      }
      ChatRequest request = packet.getData(ChatRequest.class);
      if (request == null) {
        clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Invalid request"));
        return;
      }
      String content = request.content();
      if (content == null || content.isBlank()) {
        clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Message cannot be empty"));
        return;
      }
      content = content.trim();
      if (content.length() > MAX_MESSAGE_LENGTH) {
        clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Message too long"));
        return;
      }
      User user = clientHandler.getUser();
      // KHÔNG trust sender từ client
      ChatResponse response =
          new ChatResponse(user.getId(), user.getName(), content, LocalDateTime.now());
      PacketRes chatPacket = PacketRes.of(PacketType.CHAT, response);
      // gửi lại cho sender
      clientHandler.sendPacket(chatPacket);
      // broadcast cho user khác
      Server.broadcast(chatPacket, user.getId());
      logger.info("User {} sent chat message", user.getId());
    } catch (Exception e) {
      logger.error("Chat command failed", e);
      clientHandler.sendPacket(PacketRes.error(PacketType.CHAT, "Failed to send message"));
    }
  }
}
