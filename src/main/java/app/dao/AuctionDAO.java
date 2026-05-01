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

                i.id AS item_id,
                i.name AS item_name,
                i.seller_id AS item_seller_id,
                i.description AS item_description,
                i.starting_price,
                i.current_price,
                i.step_price,
                i.category,

                u.id AS seller_id,
                u.username,
                u.password,
                u.full_name,
                u.assets,
                u.role,

                w.id AS winner_id,
                w.username AS winner_username,
                w.password AS winner_password,
                w.full_name AS winner_full_name,
                w.assets AS winner_assets,
                w.role AS winner_role

            FROM auction_sessions s

            JOIN items i
                ON s.item_id = i.id

            JOIN users u
                ON s.seller_id = u.id
            LEFT JOIN users w
                ON s.winner_id = w.id
            """;

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

  private User mapUser(ResultSet rs) throws SQLException {

    return UserFactory.createUser(
        rs.getInt("seller_id"),
        rs.getString("full_name"),
        new Account(rs.getString("username"), rs.getString("password")),
        new Wallet(rs.getDouble("assets")),
        UserRole.valueOf(rs.getString("role")));
  }

  private Auction mapAuction(ResultSet rs) throws SQLException {

    Item item = mapItem(rs);

    User seller = mapUser(rs);

    User winner = mapWinner(rs);

    return new Auction(
        rs.getInt("id"),
        item,
        seller,
        winner,
        AuctionStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("start_time").toLocalDateTime(),
        rs.getTimestamp("end_time").toLocalDateTime(),
        rs.getDouble("highest_bid"));
  }

  public Optional<Auction> findById(Integer id) {

    String sql =
        BASE_SELECT
            + """
                WHERE s.id = ?
                """;

    return findOne(sql, id);
  }

  public List<Auction> findAll() {

    String sql =
        BASE_SELECT
            + """
                ORDER BY s.id DESC
                """;

    return findMany(sql);
  }

  public List<Auction> findByStatus(AuctionStatus status) {

    String sql =
        BASE_SELECT
            + """
                WHERE s.status = ?
                ORDER BY s.end_time ASC
                """;

    return findMany(sql, status.name());
  }

  public List<Auction> findBySeller(Integer sellerId) {

    String sql =
        BASE_SELECT
            + """
                WHERE s.seller_id = ?
                ORDER BY s.id DESC
                """;

    return findMany(sql, sellerId);
  }

  public Auction save(Auction auction) {

    String sql =
        """
                INSERT INTO auction_sessions
                (
                    item_id,
                    seller_id,
                    winner_id,
                    status,
                    start_time,
                    end_time,
                    highest_bid
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      ps.setInt(1, auction.getItem().getId());

      ps.setInt(2, auction.getSeller().getId());

      if (auction.getWinner() != null) {
        ps.setInt(3, auction.getWinner().getId());
      } else {
        ps.setNull(3, Types.INTEGER);
      }

      ps.setString(4, auction.getStatus().name());

      ps.setTimestamp(5, Timestamp.valueOf(auction.getStartTime()));

      ps.setTimestamp(6, Timestamp.valueOf(auction.getEndTime()));

      ps.setDouble(7, auction.getHighestBid());

      int affectedRows = ps.executeUpdate();

      if (affectedRows == 0) {

        throw new DatabaseException("Không thể tạo auction.");
      }

      try (ResultSet rs = ps.getGeneratedKeys()) {

        if (rs.next()) {
          auction.setId(rs.getInt(1));
        }

        return auction;
      }

    } catch (SQLException e) {

      throw new DatabaseException("Lỗi khi tạo auction.", e);
    }
  }

  public boolean updateStatus(Integer auctionId, AuctionStatus status) {

    String sql =
        """
                UPDATE auction_sessions
                SET status = ?
                WHERE id = ?
                """;

    return executeUpdate(sql, status.name(), auctionId);
  }

  public boolean updateHighestBid(Integer auctionId, Double highestBid) {

    String sql =
        """
                UPDATE auction_sessions
                SET highest_bid = ?
                WHERE id = ?
                """;

    return executeUpdate(sql, highestBid, auctionId);
  }

  public boolean updateWinner(Integer auctionId, Integer winnerId) {

    String sql =
        """
            UPDATE auction_sessions
            SET winner_id = ?
            WHERE id = ?
            """;

    return executeUpdate(sql, winnerId, auctionId);
  }

  public boolean delete(Integer id) {

    String sql =
        """
                DELETE FROM auction_sessions
                WHERE id = ?
                """;

    return executeUpdate(sql, id);
  }

  private Optional<Auction> findOne(String sql, Object... params) {

    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      setParameters(ps, params);

      try (ResultSet rs = ps.executeQuery()) {

        if (rs.next()) {
          return Optional.of(mapAuction(rs));
        }

        return Optional.empty();
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

        return auctions;
      }

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
