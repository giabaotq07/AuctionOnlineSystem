package app.dao;

import app.data.ChatResponse;
import java.util.List;

/** ChatDao. */
public interface ChatDao {
  /** save. */
  int save(ChatResponse message);

  /** getConversation. */
  List<ChatResponse> getConversation(int conversationId);
}
