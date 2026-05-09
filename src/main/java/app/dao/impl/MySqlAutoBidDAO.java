package app.dao.impl;

import app.dao.AutoBidDAO;
import app.dao.BaseDAO;
import app.exception.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySqlAutoBidDAO extends BaseDAO implements AutoBidDAO {

  public MySqlAutoBidDAO() {}

  @Override
  public void delete(int sessionId, int userId) {
    runWithConnection(
        conn -> delete(conn, sessionId, userId), "Lỗi kết nối khi xóa cấu hình auto-bid.");
  }

  @Override
  public void upsert(int sessionId, int userId, long maxBid, long increment) {
    runWithConnection(
        conn -> upsert(conn, sessionId, userId, maxBid, increment),
        "Lỗi kết nối khi lưu cấu hình auto-bid.");
  }

  private void delete(Connection conn, int sessionId, int userId) {
    String sql = "DELETE FROM auto_bids WHERE session_id = ? AND user_id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      ps.setInt(2, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi xóa cấu hình auto-bid.", e);
    }
  }

  private void upsert(Connection conn, int sessionId, int userId, long maxBid, long increment) {
    String sql =
        """
            INSERT INTO auto_bids (session_id, user_id, max_bid, increment_amount)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              max_bid = VALUES(max_bid),
              increment_amount = VALUES(increment_amount)
            """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      ps.setInt(2, userId);
      ps.setLong(3, maxBid);
      ps.setLong(4, increment);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi lưu cấu hình auto-bid.", e);
    }
  }
}
