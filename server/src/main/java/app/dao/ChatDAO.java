package app.dao;

import app.dto.ChatResponse;
import java.util.List;

/** ChatDAO. */
public interface ChatDAO {
  /** save. */
  int save(ChatResponse message);

  /** getConversation. */
  List<ChatResponse> getConversation(int conversationId);
}
