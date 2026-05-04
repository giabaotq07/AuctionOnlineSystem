package app.service;

import app.config.DatabaseConnection;
import app.dao.ItemDAO;
import app.enums.ItemStatus;
import app.exception.DatabaseException;
import app.models.Item;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ItemService {
  private ItemDAO itemDAO;

  public ItemService(ItemDAO itemDAO) {
    this.itemDAO = itemDAO;
  }

  public Item add(Item item) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      return itemDAO.save(conn, item);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi thêm item.", e);
    }
  }

  public Item getById(int id) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      return itemDAO.findById(conn, id).orElse(null);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi tải item.", e);
    }
  }

  public void update(Item item) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      itemDAO.update(conn, item);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi cập nhật item.", e);
    }
  }

  public void updateStatus(int id, ItemStatus status) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      itemDAO.updateStatus(conn, id, status);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi cập nhật trạng thái item.", e);
    }
  }

  public void delete(int id) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      itemDAO.updateStatus(conn, id, ItemStatus.DELETE);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi xóa item.", e);
    }
  }

  public List<Item> getAll() {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      return itemDAO.findAll(conn);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi tải danh sách items.", e);
    }
  }
}
