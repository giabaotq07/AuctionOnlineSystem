package app.dao;

import app.config.DatabaseConnection;
import app.enums.HistoryType;
import app.exceptions.DatabaseException;
import app.models.HistoryRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoryDAO {
  private final DatabaseConnection connection = DatabaseConnection.getInstance();
  public boolean addHistoryRecord(HistoryRecord record) {
    String query =
        "INSERT INTO history_records (session_id, type, message, time) VALUES (?, ?, ?, ?)";
    try (Connection conn = connection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, record.getSessionId());
      pstmt.setString(2, record.getType().name());
      pstmt.setString(3, record.getMessage());
      pstmt.setTimestamp(4, java.sql.Timestamp.valueOf(record.getTime()));
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi lưu lịch sử.", e);
    }
  }

  public List<HistoryRecord> getHistoryBySession(int sessionId) {
    List<HistoryRecord> records = new ArrayList<>();
    String query = "SELECT * FROM history_records WHERE session_id = ? ORDER BY time DESC";
    try (Connection conn = connection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          HistoryRecord record =
              new HistoryRecord(
                  rs.getInt("session_id"),
                  HistoryType.valueOf(rs.getString("type")),
                  rs.getString("message"),
                  rs.getTimestamp("time").toLocalDateTime());
          records.add(record);
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy xuất lịch sử.", e);
    }
    return records;
  }
}
