package app.dao;

import app.dto.ChatMessage;
import java.util.List;

public interface ChatDAO {
  int save(ChatMessage message);

  List<ChatMessage> getConversation(int conversationId);
}
