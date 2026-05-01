package app.dao;

import app.config.DatabaseConnection;
import app.enums.AuctionStatus;
import app.enums.ItemType;
import app.enums.UserRole;
import app.exception.DatabaseException;
import app.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuctionDAO {

  private static final String TABLE = "auction_sessions";

  private final DatabaseConnection databaseConnection = DatabaseConnection.getInstance();

  private static final String BASE_SELECT =
      """
          SELECT
              s.id,
              s.status,
              s.start_time,
              s.end_time,
              s.highest_bid,
              s.extended_count,

              i.id          AS item_id,
              i.name        AS item_name,
              i.seller_id   AS item_seller_id,
              i.description AS item_description,
              i.starting_price,
              i.step_price,
              i.category,

              u.id        AS seller_id,
              u.username,
              u.password,
              u.full_name,
              u.assets,
              u.role,

              w.id        AS winner_id,
              w.username  AS winner_username,
              w.password  AS winner_password,
              w.full_name AS winner_full_name,
              w.assets    AS winner_assets,
              w.role      AS winner_role

          FROM auction_sessions s
          JOIN items i ON s.item_id = i.id
          JOIN users u ON s.seller_id = u.id
          LEFT JOIN users w ON s.winner_id = w.id
          """;

  private Item mapItem(ResultSet rs) throws SQLException {
    return ItemFactory.createItem(
        rs.getInt("item_id"),
        rs.getString("item_name"),
        rs.getInt("item_seller_id"),
        rs.getString("item_description"),
        rs.getDouble("starting_price"),
        rs.getDouble("step_price"),
        ItemType.valueOf(rs.getString("category")));
  }

  private User mapSeller(ResultSet rs) throws SQLException {
    return UserFactory.createUser(
        rs.getInt("seller_id"),
        rs.getString("full_name"),
        new Account(rs.getString("username"), rs.getString("password")),
        new Wallet(rs.getDouble("assets")),
        UserRole.valueOf(rs.getString("role")));
  }

  private User mapWinner(ResultSet rs) throws SQLException {
    int winnerId = rs.getInt("winner_id");
    if (rs.wasNull()) return null;
    return UserFactory.createUser(
        winnerId,
        rs.getString("winner_full_name"),
        new Account(rs.getString("winner_username"), rs.getString("winner_password")),
        new Wallet(rs.getDouble("winner_assets")),
        UserRole.valueOf(rs.getString("winner_role")));
  }

  private Auction mapAuction(ResultSet rs) throws SQLException {
    return new Auction(
        rs.getInt("id"),
        mapItem(rs),
        mapSeller(rs),
        mapWinner(rs),
        AuctionStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("start_time").toLocalDateTime(),
        rs.getTimestamp("end_time").toLocalDateTime(),
        rs.getDouble("highest_bid"),
        rs.getInt("extended_count"));
  }

  // ── Read methods ──────────────────────────────────────────────

  public Optional<Auction> findById(int id) {
    return findOne(BASE_SELECT + "WHERE s.id = ?", id);
  }

  public List<Auction> findAll() {
    return findMany(BASE_SELECT + "ORDER BY s.id DESC");
  }

  public List<Auction> findByStatus(AuctionStatus status) {
    return findMany(BASE_SELECT + "WHERE s.status = ? ORDER BY s.end_time ASC", status.name());
  }

  public List<Auction> findBySeller(int sellerId) {
    return findMany(BASE_SELECT + "WHERE s.seller_id = ? ORDER BY s.id DESC", sellerId);
  }

  // ── Transaction methods — nhận Connection từ Service ──────────

  // Lock row để tránh race condition — chỉ dùng trong transaction
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

  public double getHighestBid(Connection conn, int sessionId) throws SQLException {
    String sql = "SELECT highest_bid FROM auction_sessions WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getDouble("highest_bid") : 0;
      }
    }
  }

  public void updateHighestBid(Connection conn, int sessionId, double highestBid)
      throws SQLException {
    String sql = "UPDATE auction_sessions SET highest_bid = ? WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setDouble(1, highestBid);
      ps.setInt(2, sessionId);
      ps.executeUpdate();
    }
  }

  // Anti-sniping — gia hạn thêm Y giây và tăng extended_count
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
      ps.setInt(1, auction.getItem().getId());
      ps.setInt(2, auction.getSeller().getId());
      ps.setString(3, auction.getStatus().name());
      ps.setTimestamp(4, Timestamp.valueOf(auction.getStartTime()));
      ps.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
      ps.setDouble(6, auction.getHighestBid());
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

  // ── Helpers ───────────────────────────────────────────────────

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
