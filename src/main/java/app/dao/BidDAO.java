package app.dao;

import app.config.DatabaseConnection;
import app.exceptions.DatabaseException;
import app.exceptions.ServiceException;
import app.models.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {
  private static BidDAO instance;

  private BidDAO() {}

  public static BidDAO getInstance() {
    if (instance == null) {
      synchronized (BidDAO.class) {
        if (instance == null) {
          instance = new BidDAO();
        }
      }
    }
    return instance;
  }

  private User mapUser(ResultSet rs) throws SQLException {
    return UserFactory.createUser(
        rs.getInt("id"),
        rs.getString("name"),
        new Account(rs.getString("account"), rs.getString("password")),
        new Wallet(),
        rs.getString("role"));
  }

  public void placeBid(int sessionId, int userId, double bidAmount) {
    // Giữ nguyên việc lấy Connection và setAutoCommit(false) của bạn
    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false);
      try {
        // 1. Lock phiên đấu giá (Giữ nguyên SQL của bạn)
        String lockSql = "SELECT id FROM auction_sessions WHERE id = ? FOR UPDATE";
        try (PreparedStatement checkStmt = conn.prepareStatement(lockSql)) {
          checkStmt.setInt(1, sessionId);
          try (ResultSet rs = checkStmt.executeQuery()) {
            if (!rs.next()) {
              throw new ServiceException("Phiên đấu giá không tồn tại!");
            }
          }
        }

        // 2. Lấy giá cao nhất (Giữ nguyên SQL của bạn)
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

        // 3. So sánh giá (Giữ nguyên logic của bạn)
        if (bidAmount <= currentMax) {
          throw new ServiceException("Giá đặt mới phải cao hơn giá hiện tại ($" + currentMax + ")");
        }

        // 4. Ghi dữ liệu - Đảm bảo tên cột 'time' khớp với DB của bạn
        String insertSql = "INSERT INTO bids (session_id, user_id, bid_amount, time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
          pstmt.setInt(1, sessionId);
          pstmt.setInt(2, userId);
          pstmt.setDouble(3, bidAmount);
          pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));

          if (pstmt.executeUpdate() == 0) {
            throw new DatabaseException("Đặt giá thất bại, không có hàng nào được thêm.");
          }
        }

        // QUAN TRỌNG: Phải commit để MySQL lưu dữ liệu
        conn.commit();
        System.out.println("[Server] Đã lưu bid vào MySQL thành công.");

      } catch (Exception e) {
        conn.rollback(); // Nếu lỗi thì trả lại trạng thái cũ
        // Giữ nguyên cách bạn ném Exception
        if (e instanceof ServiceException || e instanceof DatabaseException) throw e;
        throw new DatabaseException("Lỗi db khi thực hiện đặt giá.", e);
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối db khi thực hiện đặt giá.", e);
    }
  }

  public List<BidTransaction> getBidsBySession(int sessionId) {
    List<BidTransaction> bidTransactions = new ArrayList<>();
    String query =
        "SELECT b.bid_amount, b.time, u.id, u.name, u.account, u.password, u.assets, u.role "
            + "FROM bids b "
            + "JOIN users u ON b.user_id = u.id "
            + "WHERE b.session_id = ? "
            + "ORDER BY b.bid_amount DESC LIMIT 1";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          User bidder = mapUser(rs);
          double amount = rs.getDouble("bid_amount");
          LocalDateTime time = rs.getTimestamp("time").toLocalDateTime();
          bidTransactions.add(new BidTransaction(bidder, amount, time));
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi lấy danh sách đặt giá của phiên: " + sessionId, e);
    }
    return bidTransactions;
  }

  public BidTransaction getHighestBid(int sessionId) {
    String query =
        "SELECT b.bid_amount, b.time, u.id, u.name, u.account, u.password, u.assets, u.role "
            + "FROM bids b "
            + "JOIN users u ON b.user_id = u.id "
            + "WHERE b.session_id = ? "
            + "ORDER BY b.bid_amount DESC LIMIT 1";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          User bidder = mapUser(rs);
          return new BidTransaction(
              bidder, rs.getDouble("bid_amount"), rs.getTimestamp("time").toLocalDateTime());
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi lấy giá cao nhất của phiên: " + sessionId, e);
    }
    return null;
  }

  // ✅ Thêm method để lấy highest bid amount (giá cao nhất) từ database
  public double getHighestBidAmount(int sessionId) {
    String query = "SELECT MAX(bid_amount) AS max_bid FROM bids WHERE session_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          double maxBid = rs.getDouble("max_bid");
          // Nếu NULL (chưa có bid nào), trả về 0
          if (rs.wasNull()) {
            return 0;
          }
          return maxBid;
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi lấy giá cao nhất của phiên: " + sessionId, e);
    }
    return 0;
  }
}
