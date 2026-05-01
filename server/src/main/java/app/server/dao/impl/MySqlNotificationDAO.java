package app.server.dao.impl;

import app.enums.NotificationType;
import app.models.Notification;
import app.server.config.connection;
import app.server.dao.NotificationDAO;
import app.server.exception.DaoException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MySqlNotificationDAO implements NotificationDAO {
  private final connection connection = connection.getInstance();

  @Override
  public int save(Notification notification) {
  String sql =
   "INSERT INTO notifications(user_id, type, content, is_read, created_at) VALUES(?,?,?,?,?)";
  try (Connection conn = connection.getConnection();
   PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
   stmt.setInt(1, notification.getUserId());
   stmt.setString(2, notification.getType().name());
   stmt.setString(3, notification.getContent());
   stmt.setBoolean(4, notification.isRead());
   stmt.setTimestamp(5, Timestamp.valueOf(notification.getCreatedAt()));
   stmt.executeUpdate();
   try (ResultSet rs = stmt.getGeneratedKeys()) {
   if (rs.next()) {
    return rs.getInt(1);
   }
   }
   return 0;
  } catch (SQLException e) {
   throw new DaoException("Failed to save notification", e);
  }
  }

  @Override
  public List<Notification> findByUser(int userId) {
  String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
  try (Connection conn = connection.getConnection();
   PreparedStatement stmt = conn.prepareStatement(sql)) {
   stmt.setInt(1, userId);
   try (ResultSet rs = stmt.executeQuery()) {
   List<Notification> notifications = new ArrayList<>();
   while (rs.next()) {
    notifications.add(mapNotification(rs));
   }
   return notifications;
   }
  } catch (SQLException e) {
   throw new DaoException("Failed to load notifications", e);
  }
  }

  private Notification mapNotification(ResultSet rs) throws SQLException {
  int id = rs.getInt("id");
  int userId = rs.getInt("user_id");
  NotificationType type = NotificationType.valueOf(rs.getString("type"));
  String content = rs.getString("content");
  boolean read = rs.getBoolean("is_read");
  LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
  Notification notification = new Notification(id, userId, type, content, read);
  if (read) {
   notification.markRead();
  }
  return notification;
  }
}

