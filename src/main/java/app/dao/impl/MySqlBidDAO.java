package app.dao.impl;

import app.dao.BaseDAO;
import app.dao.BidDAO;
import app.exception.DatabaseException;
import app.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlBidDAO extends BaseDAO implements BidDAO {
  public MySqlBidDAO() {}

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

  @Override
  public void insertBid(int sessionId, int userId, long bidAmount, boolean isAutoBid)
      throws DatabaseException {
    runWithConnection(
        conn -> insertBid(conn, sessionId, userId, bidAmount, isAutoBid),
        "Lỗi kết nối khi thêm bid.");
  }

  @Override
  public void insertBid(
      Connection conn, int sessionId, int userId, long bidAmount, boolean isAutoBid) {
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
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi thêm bid.", e);
    }
  }

  @Override
  public Optional<BidTransaction> findHighestBid(int sessionId) {
    return withConnection(
        conn -> findHighestBid(conn, sessionId), "Lỗi kết nối khi truy vấn bid cao nhất.");
  }

  @Override
  public Optional<BidTransaction> findHighestBid(Connection conn, int sessionId) {
    String sql = BID_SELECT + " ORDER BY b.bid_amount DESC, b.bid_time DESC LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapBid(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi truy vấn bid cao nhất.", e);
    }
  }

  @Override
  public List<BidTransaction> findBySession(int sessionId) {
    return withConnection(
        conn ->
            queryBids(conn, BID_SELECT + " ORDER BY b.bid_amount DESC, b.bid_time DESC", sessionId),
        "Lỗi kết nối khi truy vấn bids.");
  }

  @Override
  public List<BidTransaction> findBySessionForChart(int sessionId) {
    return withConnection(
        conn -> queryBids(conn, BID_SELECT + " ORDER BY b.bid_time ASC", sessionId),
        "Lỗi kết nối khi truy vấn bids.");
  }

  @Override
  public boolean existsBySessionAndUser(int sessionId, int userId) {
    return withConnection(
        conn -> existsBySessionAndUser(conn, sessionId, userId),
        "Lỗi kết nối khi kiểm tra tồn tại bid.");
  }

  private boolean existsBySessionAndUser(Connection conn, int sessionId, int userId) {
    String sql = "SELECT 1 FROM bids WHERE session_id = ? AND user_id = ? LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      ps.setInt(2, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi kiểm tra tồn tại bid.", e);
    }
  }

  // ── Private helpers ───────────────────────────────────────────

  private List<BidTransaction> queryBids(Connection conn, String sql, int sessionId) {
    List<BidTransaction> bids = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) bids.add(mapBid(rs));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi truy vấn bids.", e);
    }
    return bids;
  }
}
