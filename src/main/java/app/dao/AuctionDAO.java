package app.dao;

import app.config.DatabaseConnection;
import app.enums.AuctionStatus;
import app.exception.DatabaseException;
import app.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuctionDAO {
  private final DatabaseConnection databaseConnection = DatabaseConnection.getInstance();

  private static final String TABLE = "auction_sessions";

  private static final String BASE_SELECT =
      """
              SELECT
                  s.id,
                  s.status,
                  s.start_time,
                  s.end_time,
                  s.highest_bid,
                  s.extended_count,
                  s.created_at,
                  s.updated_at,
                  i.id  AS item_id,
                  u.id  AS seller_id,
                  w.id  AS winner_id
              FROM auction_sessions s
              JOIN items i ON s.item_id = i.id
              JOIN users u ON s.seller_id = u.id
              LEFT JOIN users w ON s.winner_id = w.id
              """;

  public AuctionDAO() {}

  private Auction mapAuction(ResultSet rs) throws SQLException {
    return new Auction(
        rs.getInt("id"),
        rs.getInt("item_id"),
        rs.getInt("seller_id"),
        (Integer) rs.getObject("winner_id"),
        AuctionStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("start_time").toLocalDateTime(),
        rs.getTimestamp("end_time").toLocalDateTime(),
        rs.getLong("highest_bid"),
        rs.getInt("extended_count"),
        rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("updated_at").toLocalDateTime());
  }

  // ── Read methods ──────────────────────────────────────────────

  public Optional<Auction> findById(int id) {
    return findOne(BASE_SELECT + " WHERE s.id = ?", id);
  }

  public List<Auction> findAll() {
    return findMany(BASE_SELECT + " ORDER BY s.id DESC");
  }

  public List<Auction> findByStatus(AuctionStatus status) {
    return findMany(BASE_SELECT + " WHERE s.status = ? ORDER BY s.end_time ASC", status.name());
  }

  public List<Auction> findBySeller(int sellerId) {
    return findMany(BASE_SELECT + " WHERE s.seller_id = ? ORDER BY s.id DESC", sellerId);
  }

  // ── Transaction methods — nhận Connection từ Service ──────────

  public void lockSession(Connection conn, int sessionId) throws SQLException {
    String sql =
        """
            SELECT id FROM auction_sessions
            WHERE id = ? AND status = 'RUNNING'
            FOR UPDATE
            """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          throw new DatabaseException(
              "Phiên đấu giá không tồn tại hoặc không ở trạng thái RUNNING.");
        }
      }
    }
  }

  public long getHighestBid(Connection conn, int sessionId) throws SQLException {
    String sql = "SELECT highest_bid FROM auction_sessions WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getLong("highest_bid") : 0L;
      }
    }
  }

  public void updateHighestBid(Connection conn, int sessionId, long highestBid)
      throws SQLException {
    String sql = "UPDATE auction_sessions SET highest_bid = ? WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, highestBid);
      ps.setInt(2, sessionId);
      ps.executeUpdate();
    }
  }

  public void extendEndTime(Connection conn, int sessionId, int extraSeconds) throws SQLException {
    String sql =
        """
            UPDATE auction_sessions
            SET end_time = DATE_ADD(end_time, INTERVAL ? SECOND),
                extended_count = extended_count + 1
            WHERE id = ?
            """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, extraSeconds);
      ps.setInt(2, sessionId);
      ps.executeUpdate();
    }
  }

  // ── Write methods ─────────────────────────────────────────────

  public Auction save(Auction auction) {
    String sql =
        """
            INSERT INTO auction_sessions
                (item_id, seller_id, status, start_time, end_time, highest_bid)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, auction.getItemId());
      ps.setInt(2, auction.getSellerId());
      ps.setString(3, auction.getStatus().name());
      ps.setTimestamp(4, Timestamp.valueOf(auction.getStartTime()));
      ps.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
      ps.setLong(6, auction.getHighestBid());
      if (ps.executeUpdate() == 0) {
        throw new DatabaseException("Không thể tạo auction.");
      }
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) auction.setId(rs.getInt(1));
      }
      return auction;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi tạo auction.", e);
    }
  }

  public boolean updateStatus(int auctionId, AuctionStatus status) {
    return executeUpdate(
        "UPDATE auction_sessions SET status = ? WHERE id = ?", status.name(), auctionId);
  }

  public boolean updateWinner(int auctionId, int winnerId) {
    return executeUpdate(
        "UPDATE auction_sessions SET winner_id = ? WHERE id = ?", winnerId, auctionId);
  }

  public boolean delete(int id) {
    return executeUpdate("DELETE FROM auction_sessions WHERE id = ?", id);
  }

  // ── Private helpers ───────────────────────────────────────────

  private Optional<Auction> findOne(String sql, Object... params) {
    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapAuction(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private List<Auction> findMany(String sql, Object... params) {
    List<Auction> auctions = new ArrayList<>();
    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          auctions.add(mapAuction(rs));
        }
      }
      return auctions;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn danh sách auctions.", e);
    }
  }

  private boolean executeUpdate(String sql, Object... params) {
    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi cập nhật bảng " + TABLE, e);
    }
  }

  private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      ps.setObject(i + 1, params[i]);
    }
  }
}
