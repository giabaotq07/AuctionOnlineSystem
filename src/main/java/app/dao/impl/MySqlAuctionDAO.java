package app.dao.impl;

import app.dao.AuctionDAO;
import app.dao.BaseDAO;
import app.enums.AuctionStatus;
import app.exception.DatabaseException;
import app.models.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlAuctionDAO extends BaseDAO implements AuctionDAO {

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

  public MySqlAuctionDAO() {}

  private Auction mapAuction(ResultSet rs) throws SQLException {
    return new Auction(
        rs.getInt("id"),
        rs.getInt("item_id"),
        rs.getInt("seller_id"),
        (Integer) rs.getObject("winner_id"),
        AuctionStatus.valueOf(rs.getString("status")),
        Optional.ofNullable(rs.getTimestamp("start_time"))
            .map(Timestamp::toLocalDateTime)
            .orElse(null),
        Optional.ofNullable(rs.getTimestamp("end_time"))
            .map(Timestamp::toLocalDateTime)
            .orElse(null),
        rs.getLong("highest_bid"),
        rs.getInt("extended_count"),
        rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("updated_at").toLocalDateTime());
  }

  // ── Read methods ──────────────────────────────────────────────

  @Override
  public Optional<Auction> findById(int id) {
    return withConnection(conn -> findById(conn, id), "Lỗi kết nối khi tải phiên đấu giá.");
  }

  @Override
  public Optional<Auction> findById(Connection conn, int id) {
    return findOne(conn, BASE_SELECT + " WHERE s.id = ?", id);
  }

  @Override
  public List<Auction> findAll() {
    return withConnection(
        conn -> findMany(conn, BASE_SELECT + " ORDER BY s.id DESC"),
        "Lỗi kết nối khi tải danh sách phiên đấu giá.");
  }

  @Override
  public List<Auction> findByStatus(AuctionStatus status) {
    return withConnection(
        conn ->
            findMany(
                conn, BASE_SELECT + " WHERE s.status = ? ORDER BY s.end_time ASC", status.name()),
        "Lỗi kết nối khi tải danh sách phiên theo trạng thái.");
  }

  @Override
  public List<Auction> findBySeller(int sellerId) {
    return withConnection(
        conn -> findMany(conn, BASE_SELECT + " WHERE s.seller_id = ? ORDER BY s.id DESC", sellerId),
        "Lỗi kết nối khi tải danh sách phiên của người bán.");
  }

  // ── Transaction methods — nhận Connection từ Service ──────────

  @Override
  public void lockSession(Connection conn, int sessionId) {
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
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi khóa phiên đấu giá.", e);
    }
  }

  @Override
  public long getHighestBid(int sessionId) {
    return withConnection(
        conn -> getHighestBid(conn, sessionId), "Lỗi kết nối khi lấy giá thầu cao nhất.");
  }

  @Override
  public long getHighestBid(Connection conn, int sessionId) {
    String sql = "SELECT highest_bid FROM auction_sessions WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getLong("highest_bid") : 0L;
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi lấy giá thầu cao nhất của phiên đấu giá.", e);
    }
  }

  @Override
  public void updateHighestBid(Connection conn, int sessionId, long highestBid) {
    String sql = "UPDATE auction_sessions SET highest_bid = ? WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, highestBid);
      ps.setInt(2, sessionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi cập nhật giá cao nhất.", e);
    }
  }

  @Override
  public void extendEndTime(Connection conn, int sessionId, int extraSeconds) {
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
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi gia hạn thời gian phiên đấu giá.", e);
    }
  }

  @Override
  public void updateWinner(Connection conn, int auctionId, int winnerId) {
    executeUpdate(
        conn, TABLE, "UPDATE auction_sessions SET winner_id = ? WHERE id = ?", winnerId, auctionId);
  }

  // ── Write methods ─────────────────────────────────────────────

  @Override
  public Auction save(Auction auction) {
    return withConnection(conn -> save(conn, auction), "Lỗi kết nối khi tạo auction.");
  }

  @Override
  public Auction save(Connection conn, Auction auction) {
    String sql =
        """
            INSERT INTO auction_sessions
                (item_id, seller_id, status, end_time, highest_bid)
            VALUES (?, ?, ?, ?, ?)
            """;
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, auction.getItemId());
      ps.setInt(2, auction.getSellerId());
      ps.setString(3, auction.getStatus().name());
      ps.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
      ps.setLong(5, auction.getHighestBid());
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

  @Override
  public boolean updateStatus(int auctionId, AuctionStatus status) {
    return withConnection(
        conn -> updateStatus(conn, auctionId, status),
        "Lỗi kết nối khi cập nhật trạng thái phiên đấu giá.");
  }

  @Override
  public boolean updateStatus(Connection conn, int auctionId, AuctionStatus status) {
    return executeUpdate(
        conn, "UPDATE auction_sessions SET status = ? WHERE id = ?", status.name(), auctionId);
  }

  @Override
  public void updateStartTime(int auctionId, LocalDateTime startTime) {
    runWithConnection(
        conn ->
            executeUpdate(
                conn,
                TABLE,
                "UPDATE auction_sessions SET start_time = ? WHERE id = ?",
                Timestamp.valueOf(startTime),
                auctionId),
        "Lỗi kết nối khi cập nhật thời gian bắt đầu phiên.");
  }

  @Override
  public void updateEndTime(int auctionId, LocalDateTime endTime) {
    runWithConnection(
        conn -> updateEndTime(conn, auctionId, endTime),
        "Lỗi kết nối khi cập nhật thời gian kết thúc phiên.");
  }

  @Override
  public void updateEndTime(Connection conn, int auctionId, LocalDateTime endTime) {
    executeUpdate(
        conn,
        TABLE,
        "UPDATE auction_sessions SET end_time = ? WHERE id = ?",
        Timestamp.valueOf(endTime),
        auctionId);
  }

  @Override
  public void updateWinner(int auctionId, int winnerId) {
    runWithConnection(
        conn -> updateWinner(conn, auctionId, winnerId),
        "Lỗi kết nối khi cập nhật người thắng phiên.");
  }

  @Override
  public boolean delete(int id) {
    return withConnection(
        conn -> executeUpdate(conn, TABLE, "DELETE FROM auction_sessions WHERE id = ?", id),
        "Lỗi kết nối khi xóa phiên đấu giá.");
  }

  // ── Private helpers ───────────────────────────────────────────

  private Optional<Auction> findOne(Connection conn, String sql, Object... params) {
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapAuction(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private List<Auction> findMany(Connection conn, String sql, Object... params) {
    List<Auction> auctions = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
}
