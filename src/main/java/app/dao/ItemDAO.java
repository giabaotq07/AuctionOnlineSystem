package app.dao;

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
  public ItemDAO() {}

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

  public Optional<Item> findById(Connection conn, int id) {
    String sql = BASE_SELECT + "WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapItem(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  public List<Item> findAll(Connection conn) {
    return findList(conn, BASE_SELECT + "ORDER BY id DESC");
  }

  public List<Item> findBySeller(Connection conn, int sellerId) {
    return findList(conn, BASE_SELECT + "WHERE seller_id = ? ORDER BY id DESC", sellerId);
  }

  public List<Item> findByCategory(Connection conn, ItemType type) {
    return findList(conn, BASE_SELECT + "WHERE category = ? ORDER BY id DESC", type.name());
  }

  public List<Item> findAvailable(Connection conn) {
    return findList(conn, BASE_SELECT + "WHERE status = 'AVAILABLE' ORDER BY id DESC");
  }

  public Item save(Connection conn, Item item) {
    String sql =
        """
        INSERT INTO items (seller_id, name, description, category, starting_price, step_price)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
          updateStatus(conn, item.getId(), ItemStatus.AVAILABLE);
        }
      }
      return item;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi thêm item.", e);
    }
  }

  public void update(Connection conn, Item item) {
    String sql =
        """
        UPDATE items
        SET name = ?, description = ?, starting_price = ?, step_price = ?, category = ?
        WHERE id = ?
        """;
    executeUpdate(
        conn,
        sql,
        item.getName(),
        item.getDescription(),
        item.getStartingPrice(),
        item.getStepPrice(),
        item.getType().name(),
        item.getId());
  }

  // Gọi khi item bắt đầu / kết thúc đấu giá
  public void updateStatus(Connection conn, int id, ItemStatus status) {
    executeUpdate(conn, "UPDATE items SET status = ? WHERE id = ?", status.name(), id);
  }

  private List<Item> findList(Connection conn, String sql, Object... params) {
    List<Item> items = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

  private boolean executeUpdate(Connection conn, String sql, Object... params) {
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
