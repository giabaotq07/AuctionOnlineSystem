package app.dao;

import app.models.ChatMessage;
import java.util.List;

public interface ChatDAO {
  int save(ChatMessage message);

  List<ChatMessage> getConversation(int conversationId);
}
