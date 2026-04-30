package app.dao;

import app.config.DatabaseConnection;
import app.enums.AuctionStatus;
import app.enums.ItemType;
import app.enums.UserRole;
import app.exceptions.DatabaseException;
import app.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
  // 1. Chuỗi SQL JOIN dùng chung
  private final String SELECT_JOIN_QUERY =
      "SELECT s.*, "
          + "i.name AS item_name, i.description AS item_desc, i.starting_price, i.step_price, i.type AS item_type, "
          + "u.name AS seller_name, u.account AS seller_acc, u.assets AS seller_assets, u.role AS seller_role "
          + "FROM auction_sessions s "
          + "JOIN items i ON s.item_id = i.id "
          + "JOIN users u ON s.seller_id = u.id";

  private Auction mapAuction(ResultSet rs) throws SQLException {
    Item item =
        ItemFactory.createItem(
            rs.getString("item_name"),
            rs.getString("item_desc"),
            rs.getDouble("starting_price"),
            rs.getDouble("step_price"),
            ItemType.valueOf(rs.getString("item_type")));
    item.setId(rs.getInt("item_id"));
    User seller =
        UserFactory.createUser(
            rs.getString("seller_name"),
            new Account(rs.getString("seller_acc"), null),
            new Wallet(rs.getDouble("seller_assets")),
            UserRole.valueOf(rs.getString("seller_role")));
    seller.setId(rs.getInt("seller_id"));
    Auction session =
        new Auction(rs.getInt("id"), item, seller, rs.getTimestamp("end_time").toLocalDateTime());
    session.setStatus(AuctionStatus.valueOf(rs.getString("status")));
    return session;
  }

  public Auction getAuctionById(int sessionId) {
    String query = SELECT_JOIN_QUERY + " WHERE s.id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return mapAuction(rs);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi lấy phiên đấu giá.", e);
    }
  }

  public List<Auction> getAllAuction() {
    List<Auction> sessions = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(SELECT_JOIN_QUERY);
        ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        sessions.add(mapAuction(rs));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi lấy danh sách.", e);
    }
    return sessions;
  }

  public boolean updateAuctionStatus(int sessionId, AuctionStatus status) {
    String query = "UPDATE auction_sessions SET status = ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, status.name());
      pstmt.setInt(2, sessionId);
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi cập nhật trạng thái.", e);
    }
  }

  public Auction addAuction(Auction session) {
    String query =
        "INSERT INTO auction_sessions (item_id, seller_id, status, end_time) VALUES (?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setInt(1, session.getItem().getId());
      pstmt.setInt(2, session.getSeller().getId());
      pstmt.setString(3, session.getStatus().name());
      pstmt.setTimestamp(4, Timestamp.valueOf(session.getEndTime()));
      pstmt.executeUpdate();
      try (ResultSet rs = pstmt.getGeneratedKeys()) {
        if (rs.next()) {
          session.setId(rs.getInt(1));
          return session;
        }
        throw new DatabaseException("Thêm thất bại, không lấy được ID.");
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi tạo phiên đấu giá mới.", e);
    }
  }
}
