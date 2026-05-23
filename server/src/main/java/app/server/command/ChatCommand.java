package app.server.command;

import app.common.dto.ChatRequest;
import app.common.dto.ChatResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import java.time.LocalDateTime;

/** ChatCommand. */
public class ChatCommand extends SafeCommand {
  private static final int MAX_MESSAGE_LENGTH = 500;

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    ChatRequest request = requirePayload(packet, ChatRequest.class, "Tin nhắn không hợp lệ.");
    String content = request.content();
    if (content == null || content.isBlank()) {
      throw new ValidationException("Message cannot be empty");
    }
    content = content.trim();
    if (content.length() > MAX_MESSAGE_LENGTH) {
      throw new ValidationException("Message too long");
    }
    User user = requireUser(clientHandler);
    ChatResponse response =
        new ChatResponse(user.getId(), user.getName(), content, LocalDateTime.now());
    PacketRes chatPacket = PacketRes.of(ResponseType.CHAT_MESSAGE, "OK", response);
    clientHandler.sendPacket(chatPacket);
    try {
      Server.broadcast(chatPacket, user.getId());
    } catch (Exception e) {
      logger.warn("Chat message from user {} accepted, but broadcast failed", user.getId(), e);
    }
    logger.info("User {} sent chat message", user.getId());
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.CHAT_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Failed to send message";
  }
}
