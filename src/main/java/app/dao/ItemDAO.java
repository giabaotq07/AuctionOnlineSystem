package app.dao;

import app.config.DatabaseConnection;
import app.enums.ItemStatus;
import app.enums.ItemType;
import app.exception.DatabaseException;
import app.models.Item;
import app.models.ItemFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAO {
  private final DatabaseConnection databaseConnection = DatabaseConnection.getInstance();

  private static final String TABLE = "items";

  private static final String BASE_SELECT =
      """
          SELECT id, name, seller_id, description, category,
                 starting_price, step_price, status
          FROM items
      """;

  private Item mapItem(ResultSet rs) throws SQLException {
    return ItemFactory.createItem(
        rs.getInt("id"),
        rs.getString("name"),
        rs.getInt("seller_id"),
        rs.getString("description"),
        rs.getLong("starting_price"),
        rs.getLong("step_price"),
        ItemType.valueOf(rs.getString("category")));
  }

  public Optional<Item> findById(int id) {
    String sql = BASE_SELECT + "WHERE id = ?";
    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapItem(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  public List<Item> findAll() {
    return findList(BASE_SELECT + "ORDER BY id DESC");
  }

  public List<Item> findBySeller(int sellerId) {
    return findList(BASE_SELECT + "WHERE seller_id = ? ORDER BY id DESC", sellerId);
  }

  public List<Item> findByCategory(ItemType type) {
    return findList(BASE_SELECT + "WHERE category = ? ORDER BY id DESC", type.name());
  }

  // Chỉ lấy item chưa có phiên đấu giá — dùng cho màn hình tạo phiên của Seller
  public List<Item> findAvailable() {
    return findList(BASE_SELECT + "WHERE status = 'AVAILABLE' ORDER BY id DESC");
  }

  public Item save(Item item) {
    String sql =
        """
        INSERT INTO items (seller_id, name, description, category, starting_price, step_price)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, item.getSellerId());
      ps.setString(2, item.getName());
      ps.setString(3, item.getDescription());
      ps.setString(4, item.getType().name());
      ps.setLong(5, item.getStartingPrice());
      ps.setLong(6, item.getStepPrice());
      if (ps.executeUpdate() == 0) {
        throw new DatabaseException("Không thể thêm item.");
      }
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          item.setId(rs.getInt(1));
        }
      }
      return item;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi thêm item.", e);
    }
  }

  public boolean update(Item item) {
    String sql =
        """
        UPDATE items
        SET name = ?, description = ?, starting_price = ?, step_price = ?, category = ?
        WHERE id = ?
        """;
    return executeUpdate(
        sql,
        item.getName(),
        item.getDescription(),
        item.getStartingPrice(),
        item.getStepPrice(),
        item.getType().name(),
        item.getId());
  }

  // Gọi khi item bắt đầu / kết thúc đấu giá
  public boolean updateStatus(int id, ItemStatus status) {
    return executeUpdate("UPDATE items SET status = ? WHERE id = ?", status.name(), id);
  }

  public boolean delete(int id) {
    return executeUpdate("DELETE FROM items WHERE id = ?", id);
  }

  private List<Item> findList(String sql, Object... params) {
    List<Item> items = new ArrayList<>();
    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          items.add(mapItem(rs));
        }
      }
      return items;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private boolean executeUpdate(String sql, Object... params) {
    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      ps.setObject(i + 1, params[i]);
    }
  }
}
