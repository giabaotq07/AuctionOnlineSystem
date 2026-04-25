package app.dao;

import app.config.DatabaseConnection;
import app.models.AuctionSession;
import app.models.AuctionStatus;
import app.models.Item;
import app.models.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionSessionDAO {
  private final ItemDAO itemDAO = new ItemDAO();
  private final UserDAO userDAO = new UserDAO();

  public AuctionSession addAuctionSession(AuctionSession session) {
    String query =
        "INSERT INTO auction_sessions (item_id, seller_id, status, end_time) VALUES (?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setInt(1, session.getItem().getId());
      pstmt.setInt(2, session.getSeller().getId());
      pstmt.setString(3, session.getStatus().name());
      pstmt.setTimestamp(4, Timestamp.valueOf(session.getEndTime()));
      if (pstmt.executeUpdate() > 0) {
        try (ResultSet rs = pstmt.getGeneratedKeys()) {
          if (rs.next()) {
            session.setId(rs.getInt(1));
            return session;
          }
        }
      }
    } catch (SQLException | NullPointerException e) {
      e.printStackTrace();
    }
    return null;
  }

  public AuctionSession getAuctionSessionById(int sessionId) {
    String query = "SELECT * FROM auction_sessions WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          Item item = itemDAO.getItemById(rs.getInt("item_id"));
          User seller = userDAO.getUserById(rs.getInt("seller_id"));
          LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
          AuctionSession session = new AuctionSession(rs.getInt("id"), item, seller, endTime);
          session.setStatus(AuctionStatus.valueOf(rs.getString("status")));
          return session;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public boolean updateAuctionSessionStatus(int sessionId, AuctionStatus status) {
    String query = "UPDATE auction_sessions SET status = ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, status.name());
      pstmt.setInt(2, sessionId);
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      return false;
    }
  }

  public List<AuctionSession> getAllAuctionSessions() {
    List<AuctionSession> sessions = new ArrayList<>();
    String query = "SELECT * FROM auction_sessions";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query);
        ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        Item item = itemDAO.getItemById(rs.getInt("item_id"));
        User seller = userDAO.getUserById(rs.getInt("seller_id"));
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
        AuctionSession session = new AuctionSession(rs.getInt("id"), item, seller, endTime);
        session.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        sessions.add(session);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return sessions;
  }
}
