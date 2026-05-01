package app.dao;

import app.exception.DatabaseException;
import app.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BidDAO {

  private static final String BID_SELECT =
          """
          SELECT
              b.id,
              b.bid_amount,
              b.bid_time,
              b.is_auto_bid,
              u.id   AS user_id,
              u.full_name
          FROM bids b
          JOIN users u ON b.user_id = u.id
          WHERE b.session_id = ?
          """;

  private BidTransaction mapBid(ResultSet rs) throws SQLException {
    return new BidTransaction(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("full_name"),
            rs.getLong("bid_amount"),
            rs.getTimestamp("bid_time").toLocalDateTime(),
            rs.getBoolean("is_auto_bid"));
  }

  public void insertBid(
          Connection conn, int sessionId, int userId, long bidAmount, boolean isAutoBid)
          throws SQLException {
    String sql =
            """
            INSERT INTO bids (session_id, user_id, bid_amount, is_auto_bid)
            VALUES (?, ?, ?, ?)
            """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      ps.setInt(2, userId);
      ps.setLong(3, bidAmount);
      ps.setBoolean(4, isAutoBid);
      if (ps.executeUpdate() == 0) {
        throw new DatabaseException("Không thể thêm bid.");
      }
    }
  }

  public Optional<BidTransaction> findHighestBid(Connection conn, int sessionId)
          throws SQLException {
    String sql =
            BID_SELECT + " ORDER BY b.bid_amount DESC, b.bid_time DESC LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapBid(rs)) : Optional.empty();
      }
    }
  }

  public List<BidTransaction> findBySession(Connection conn, int sessionId) throws SQLException {
    return queryBids(conn, BID_SELECT + " ORDER BY b.bid_amount DESC, b.bid_time DESC", sessionId);
  }

  public List<BidTransaction> findBySessionForChart(Connection conn, int sessionId)
          throws SQLException {
    return queryBids(conn, BID_SELECT + " ORDER BY b.bid_time ASC", sessionId);
  }

  public boolean existsBySessionAndUser(Connection conn, int sessionId, int userId)
          throws SQLException {
    String sql = "SELECT 1 FROM bids WHERE session_id = ? AND user_id = ? LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      ps.setInt(2, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  // ── Private helpers ───────────────────────────────────────────

  private List<BidTransaction> queryBids(Connection conn, String sql, int sessionId)
          throws SQLException {
    List<BidTransaction> bids = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) bids.add(mapBid(rs));
      }
    }
    return bids;
  }
}