package app.dao;

import app.config.DatabaseConnection;
import app.enums.ItemType;
import app.exception.DatabaseException;
import app.models.Item;
import app.models.ItemFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAO {

  private static final String TABLE = "items";

  private final DatabaseConnection databaseConnection = DatabaseConnection.getInstance();

  // Convert dữ liệu SQL → object Java.
  private Item mapItem(ResultSet rs) throws SQLException {

    return ItemFactory.createItem(
        rs.getInt("id"),
        rs.getString("name"),
        rs.getInt("seller_id"),
        rs.getString("description"),
        rs.getDouble("starting_price"),
        rs.getDouble("step_price"),
        ItemType.valueOf(rs.getString("category")));
  }

  public Optional<Item> findById(Integer id) {

    String sql =
        """
                SELECT *
                FROM items
                WHERE id = ?
                """;

    return findOne(sql, id);
  }

  public List<Item> findAll() {

    String sql =
        """
                SELECT *
                FROM items
                ORDER BY id DESC
                """;

    List<Item> items = new ArrayList<>();

    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        items.add(mapItem(rs));
      }

      return items;

    } catch (SQLException e) {

      throw new DatabaseException("Không thể lấy danh sách items.", e);
    }
  }

  public List<Item> findByCategory(ItemType type) {

    String sql =
        """
                SELECT *
                FROM items
                WHERE category = ?
                ORDER BY id DESC
                """;

    List<Item> items = new ArrayList<>();

    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, type.name());

      try (ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
          items.add(mapItem(rs));
        }

        return items;
      }

    } catch (SQLException e) {

      throw new DatabaseException("Không thể lấy item theo category.", e);
    }
  }

  public Item save(Item item) {
    String sql =
        """
                INSERT INTO items
                (
                    name,
                    seller_id,
                    description,
                    category,
                    starting_price,
                    step_price,
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, item.getName());
      ps.setInt(2, item.getSellerId());
      ps.setString(3, item.getDescription());
      ps.setString(4, item.getType().name());
      ps.setDouble(5, item.getStartingPrice());
      ps.setDouble(6, item.getStepPrice());

      int affectedRows = ps.executeUpdate();
      if (affectedRows == 0) {
        throw new DatabaseException("Không thể thêm item.");
      }
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          item.setId(rs.getInt(1));
        }
        return item;
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi thêm item.", e);
    }
  }

  public boolean update(Item item) {

    String sql =
        """
                UPDATE items
                SET name = ?,
                    description = ?,
                    starting_price = ?,
                    step_price = ?,
                    category = ?
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

  public boolean delete(Integer id) {

    String sql =
        """
                DELETE FROM items
                WHERE id = ?
                """;

    return executeUpdate(sql, id);
  }

  // tránh lặp cho các method find
  private Optional<Item> findOne(String sql, Object... params) {

    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      setParameters(ps, params);

      try (ResultSet rs = ps.executeQuery()) {

        if (rs.next()) {
          return Optional.of(mapItem(rs));
        }

        return Optional.empty();
      }

    } catch (SQLException e) {

      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  // tránh lặp cho các phương thức update
  private boolean executeUpdate(String sql, Object... params) {

    try (Connection conn = databaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      setParameters(ps, params);

      return ps.executeUpdate() > 0;

    } catch (SQLException e) {

      throw new DatabaseException("Lỗi cập nhật bảng " + TABLE, e);
    }
  }

  // truyền tham số động cho PreparedStatement
  private void setParameters(PreparedStatement ps, Object... params) throws SQLException {

    for (int i = 0; i < params.length; i++) {
      ps.setObject(i + 1, params[i]);
    }
  }
}
