package app.dao;

import app.config.DatabaseConnection;
import app.enums.ItemType;
import app.exceptions.DatabaseException;
import app.models.Item;
import app.models.ItemFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
  private static ItemDAO instance;
  private ItemDAO() {}
  public static ItemDAO getInstance() {
    if (instance == null) {
        synchronized (ItemDAO.class) {
            if (instance == null) {
            instance = new ItemDAO();
            }
        }
    }
    return instance;
  }
  private Item mapItem(ResultSet rs) throws SQLException {
    return ItemFactory.createItem(
        rs.getInt("id"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getDouble("starting_price"),
        rs.getDouble("step_price"),
        ItemType.valueOf(rs.getString("type")));
  }

  public Item getById(Integer id) {
    String query = "SELECT * FROM items WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return mapItem(rs);
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Database/Service error", e);
    }
    return null;
  }

  public List<Item> getAll() {
    List<Item> items = new ArrayList<>();
    String query = "SELECT * FROM items";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query);
        ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        items.add(mapItem(rs));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Database/Service error", e);
    }
    return items;
  }

  public Item addItem(Item item) {
    String query =
        "INSERT INTO items (name, description, starting_price, step_price, type) VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, item.getName());
      pstmt.setString(2, item.getDescription());
      pstmt.setDouble(3, item.getStartingPrice());
      pstmt.setDouble(4, item.getStepPrice());
      pstmt.setString(5, item.getType().name());
      int affectedRows = pstmt.executeUpdate();
      if (affectedRows > 0) {
        try (ResultSet rs = pstmt.getGeneratedKeys()) {
          if (rs.next()) {
            item.setId(rs.getInt(1));
            return item;
          }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Database/Service error", e);
    }
    return null;
  }

  public boolean update(Item item) {
    String query =
        "UPDATE items SET name = ?, description = ?, starting_price = ?, step_price = ?, type = ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, item.getName());
      pstmt.setString(2, item.getDescription());
      pstmt.setDouble(3, item.getStartingPrice());
      pstmt.setDouble(4, item.getStepPrice());
      pstmt.setString(5, item.getType().name());
      pstmt.setInt(6, item.getId());
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Database/Service error", e);
    }
  }

  public boolean delete(Integer id) {
    String query = "DELETE FROM items WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      return false;
    }
  }
}
