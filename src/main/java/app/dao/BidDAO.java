package app.dao;

import app.enums.UserRole;
import app.exception.DatabaseException;
import app.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BidDAO {

  private User mapUser(ResultSet rs) throws SQLException {
    return UserFactory.createUser(
            rs.getInt("user_id"),
            rs.getString("full_name"),
            new Account(rs.getString("username"), rs.getString("password")),
            new Wallet(rs.getDouble("assets")),
            UserRole.valueOf(rs.getString("role")));
  }

  private BidTransaction mapBid(ResultSet rs) throws SQLException {
    return new BidTransaction(
            mapUser(rs),
            rs.getDouble("bid_amount"),
            rs.getTimestamp("bid_time").toLocalDateTime());
  }

  // Dùng trong transaction — nhận Connection từ Service
  public void insertBid(Connection conn, int sessionId, int userId, double bidAmount)
          throws SQLException {
    String sql = """
        INSERT INTO bids (session_id, user_id, bid_amount)
        VALUES (?, ?, ?)
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      ps.setInt(2, userId);
      ps.setDouble(3, bidAmount);
      if (ps.executeUpdate() == 0) {
        throw new DatabaseException("Không thể thêm bid.");
      }
    }
  }

  public Optional<BidTransaction> findHighestBid(Connection conn, int sessionId)
          throws SQLException {
    String sql = """
        SELECT
            b.bid_amount,
            b.bid_time,
            u.id   AS user_id,
            u.username,
            u.password,
            u.full_name,
            u.assets,
            u.role
        FROM bids b
        JOIN users u ON b.user_id = u.id
        WHERE b.session_id = ?
        ORDER BY b.bid_amount DESC, b.bid_time DESC
        LIMIT 1
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapBid(rs)) : Optional.empty();
      }
    }
  }

  public List<BidTransaction> findBySession(Connection conn, int sessionId)
          throws SQLException {
    String sql = """
        SELECT
            b.bid_amount,
            b.bid_time,
            u.id   AS user_id,
            u.username,
            u.password,
            u.full_name,
            u.assets,
            u.role
        FROM bids b
        JOIN users u ON b.user_id = u.id
        WHERE b.session_id = ?
        ORDER BY b.bid_amount DESC, b.bid_time DESC
        """;
    List<BidTransaction> bids = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          bids.add(mapBid(rs));
        }
      }
    }
    return bids;
  }
}