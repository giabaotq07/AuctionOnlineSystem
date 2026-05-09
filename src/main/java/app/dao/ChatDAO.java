package app.dao;

import app.data.ChatResponse;
import java.util.List;

public interface ChatDAO {
  int save(ChatResponse message);

  List<ChatResponse> getConversation(int conversationId);
}
