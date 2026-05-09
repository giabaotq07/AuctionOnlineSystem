package app.dao.impl;

import app.dao.BaseDAO;
import app.dao.BidDAO;
import app.enums.AuctionStatus;
import app.exception.DatabaseException;
import app.exception.ServiceException;
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

  private void insertBid(
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

  private Optional<BidTransaction> findHighestBid(Connection conn, int sessionId) {
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

  @Override
  public void placeBidAtomic(int sessionId, int userId, long bidAmount, long minIncrement) {
    runInTransaction(
        conn -> {
          AuctionSnapshot snapshot = lockAuctionSnapshot(conn, sessionId);
          if (snapshot.status != AuctionStatus.RUNNING) {
            throw new ServiceException(
                "Phiên đấu giá đã " + snapshot.status.name().toLowerCase() + ".");
          }
          if (bidAmount < snapshot.highestBid + minIncrement) {
            throw new ServiceException(
                "Giá đặt phải cao hơn giá hiện tại ít nhất "
                    + minIncrement
                    + " VNĐ. "
                    + "Giá tối thiểu: "
                    + (snapshot.highestBid + minIncrement));
          }

          insertBid(conn, sessionId, userId, bidAmount, false);
          updateHighestBid(conn, sessionId, bidAmount);
        },
        "Lỗi kết nối khi đặt giá.");
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

  private void updateHighestBid(Connection conn, int sessionId, long highestBid) {
    String sql = "UPDATE auction_sessions SET highest_bid = ? WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, highestBid);
      ps.setInt(2, sessionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi cập nhật giá cao nhất.", e);
    }
  }

  private AuctionSnapshot lockAuctionSnapshot(Connection conn, int sessionId) {
    String sql = "SELECT status, highest_bid FROM auction_sessions WHERE id = ? FOR UPDATE";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          throw new ServiceException("Phiên đấu giá không tồn tại.");
        }
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
        long highestBid = rs.getLong("highest_bid");
        return new AuctionSnapshot(status, highestBid);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi khóa phiên đấu giá.", e);
    }
  }

  /**
   * Immutable snapshot of auction state captured under row lock (FOR UPDATE). Prevents re-querying
   * within same transaction.
   */
  private static class AuctionSnapshot {
    private final AuctionStatus status;
    private final long highestBid;

    private AuctionSnapshot(AuctionStatus status, long highestBid) {
      this.status = status;
      this.highestBid = highestBid;
    }
  }
}
