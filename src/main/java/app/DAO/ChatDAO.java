package app.dao;

import app.data.ChatResponse;
import java.util.List;

/** ChatDAO. */
public interface ChatDAO {
  /** save. */
  int save(ChatResponse message);

  /** getConversation. */
  List<ChatResponse> getConversation(int conversationId);
}
