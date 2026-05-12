package app.service;

import app.config.DatabaseConnection;
import app.dao.ItemDAO;
import app.enums.ItemStatus;
import app.exception.DatabaseException;
import app.models.Item;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class ItemService {
  private ItemDAO itemDAO;

  public ItemService(ItemDAO itemDAO) {
    this.itemDAO = itemDAO;
  }

  public Item add(Item item) {
    return runInTransaction(conn -> itemDAO.save(conn, item));
  }

  public Optional<Item> getById(int id) {
    return itemDAO.findById(id);
  }

  public void update(Item item) {
    runInTransaction(
        conn -> {
          itemDAO.update(conn, item);
          return null;
        });
  }

  public void updateStatus(int id, ItemStatus status) {
    runInTransaction(
        conn ->
            itemDAO
                .findById(conn, id)
                .map(
                    item -> {
                      item.setStatus(status);
                      itemDAO.update(conn, item);
                      return null;
                    })
                .orElse(null));
  }

  public void delete(int id) {
    updateStatus(id, ItemStatus.DELETE);
  }

  public List<Item> getAll() {
    return itemDAO.findAll();
  }

  private <T> T runInTransaction(java.util.function.Function<Connection, T> work) {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
      conn.setAutoCommit(false);
      try {
        T result = work.apply(conn);
        conn.commit();
        return result;
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (java.sql.SQLException e) {
      throw new DatabaseException("Lỗi transaction.", e);
    }
  }
}
