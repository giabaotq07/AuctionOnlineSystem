package app.dao;

import app.config.DatabaseConnection;
import app.models.Bid;
import app.models.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {
  private final UserDAO userDAO = new UserDAO();

  public boolean placeBid(int sessionId, int userId, double bidAmount) {
    String insertSql =
        "INSERT INTO bids (session_id, user_id, bid_amount, time) VALUES (?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
      pstmt.setInt(1, sessionId);
      pstmt.setInt(2, userId);
      pstmt.setDouble(3, bidAmount);
      pstmt.setTimestamp(4, java.sql.Timestamp.valueOf(LocalDateTime.now()));
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      return false;
    }
  }

  public List<Bid> getBidsBySession(int sessionId) {
    List<Bid> bids = new ArrayList<>();
    String query = "SELECT * FROM bids WHERE session_id = ? ORDER BY time ASC";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          int userId = rs.getInt("user_id");
          double amount = rs.getDouble("bid_amount");
          LocalDateTime time = rs.getTimestamp("time").toLocalDateTime();
          User bidder = userDAO.getUserById(userId);
          bids.add(new Bid(bidder, amount, time));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return bids;
  }
}
