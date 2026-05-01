package app.server.dao.impl;

import app.models.ChatMessage;
import app.server.config.connection;
import app.server.dao.ChatDAO;
import app.server.exception.DaoException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MySqlChatDAO implements ChatDAO {
  private final connection connection = connection.getInstance();

  @Override
  public int save(ChatMessage message) {
    String sql =
        "INSERT INTO chat_messages(conversation_id, sender_id, receiver_id, content, sent_at) VALUES(?,?,?,?,?)";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setInt(1, message.getConversationId());
      stmt.setInt(2, message.getSenderId());
      stmt.setInt(3, message.getReceiverId());
      stmt.setString(4, message.getContent());
      stmt.setTimestamp(5, Timestamp.valueOf(message.getSentAt()));
      stmt.executeUpdate();
      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
      return 0;
    } catch (SQLException e) {
      throw new DaoException("Failed to save chat message", e);
    }
  }

  @Override
  public List<ChatMessage> getConversation(int conversationId) {
    String sql = "SELECT * FROM chat_messages WHERE conversation_id = ? ORDER BY sent_at ASC";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, conversationId);
      try (ResultSet rs = stmt.executeQuery()) {
        List<ChatMessage> messages = new ArrayList<>();
        while (rs.next()) {
          messages.add(mapMessage(rs));
        }
        return messages;
      }
    } catch (SQLException e) {
      throw new DaoException("Failed to load conversation", e);
    }
  }

  private ChatMessage mapMessage(ResultSet rs) throws SQLException {
    int id = rs.getInt("id");
    int conversationId = rs.getInt("conversation_id");
    int senderId = rs.getInt("sender_id");
    int receiverId = rs.getInt("receiver_id");
    String content = rs.getString("content");
    LocalDateTime sentAt = rs.getTimestamp("sent_at").toLocalDateTime();
    return new ChatMessage(id, conversationId, senderId, receiverId, content, sentAt);
  }
}

