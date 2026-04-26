package app.dao;

import app.config.DatabaseConnection;
import app.exceptions.DatabaseException;
import app.models.Bid;
import app.models.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import app.exceptions.ServiceException;

public class BidDAO {
  public void placeBid(int sessionId, int userId, double bidAmount) {
    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Lock phiên đấu giá để chống lost update / race condition
        String lockSql = "SELECT id FROM auction_sessions WHERE id = ? FOR UPDATE";
        try (PreparedStatement checkStmt = conn.prepareStatement(lockSql)) {
          checkStmt.setInt(1, sessionId);
          try (ResultSet rs = checkStmt.executeQuery()) {
            if (!rs.next()) {
              throw new ServiceException("Phiên đấu giá không tồn tại!");
            }
          }
        }

        // Lấy giá cao nhất TRONG VÒNG LOCK
        String maxSql = "SELECT MAX(bid_amount) FROM bids WHERE session_id = ?";
        double currentMax = 0;
        try (PreparedStatement maxStmt = conn.prepareStatement(maxSql)) {
          maxStmt.setInt(1, sessionId);
          try (ResultSet rs = maxStmt.executeQuery()) {
            if (rs.next()) {
              currentMax = rs.getDouble(1);
            }
          }
        }

        // Validate
        if (bidAmount <= currentMax) {
          throw new ServiceException("Giá đặt mới phải cao hơn giá hiện tại ($" + currentMax + ")");
        }

        // Ghi dữ liệu
        String insertSql =
            "INSERT INTO bids (session_id, user_id, bid_amount, time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
          pstmt.setInt(1, sessionId);
          pstmt.setInt(2, userId);
          pstmt.setDouble(3, bidAmount);
          pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
          if (pstmt.executeUpdate() == 0) {
            throw new DatabaseException("Đặt giá thất bại, không có hàng nào được thêm.");
          }
        }
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        if (e instanceof RuntimeException) throw (RuntimeException) e;
        throw new DatabaseException("Lỗi db khi thực hiện đặt giá.", e);
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối db khi thực hiện đặt giá.", e);
    }
  }

  public List<Bid> getBidsBySession(int sessionId) {
    List<Bid> bids = new ArrayList<>();
    String query =
        "SELECT b.bid_amount, b.time, u.id, u.name, u.account "
            + "FROM bids b "
            + "JOIN users u ON b.user_id = u.id "
            + "WHERE b.session_id = ? "
            + "ORDER BY b.time ASC";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          User bidder =
              new User(rs.getInt("id"), rs.getString("name"), rs.getString("account"), null);
          double amount = rs.getDouble("bid_amount");
          LocalDateTime time = rs.getTimestamp("time").toLocalDateTime();
          bids.add(new Bid(bidder, amount, time));
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi lấy danh sách đặt giá của phiên: " + sessionId, e);
    }
    return bids;
  }

  public Bid getHighestBid(int sessionId) {
    String query =
        "SELECT b.bid_amount, b.time, u.id, u.name, u.account "
            + "FROM bids b "
            + "JOIN users u ON b.user_id = u.id "
            + "WHERE b.session_id = ? "
            + "ORDER BY b.bid_amount DESC LIMIT 1";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          User bidder =
              new User(rs.getInt("id"), rs.getString("name"), rs.getString("account"), null);
          return new Bid(
              bidder, rs.getDouble("bid_amount"), rs.getTimestamp("time").toLocalDateTime());
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi lấy giá cao nhất của phiên: " + sessionId, e);
    }
    return null;
  }
}
