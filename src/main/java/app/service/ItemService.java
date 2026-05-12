package app.service;

import app.dao.ItemDAO;
import app.database.TransactionManager;
import app.enums.ItemStatus;
import app.models.Item;
import java.util.List;
import java.util.Optional;

public class ItemService {
  private ItemDAO itemDAO;
  private final TransactionManager transactionManager;

  public ItemService(ItemDAO itemDAO) {
    this.itemDAO = itemDAO;
    this.transactionManager = new  TransactionManager();
  }

  public Item add(Item item) {
    return transactionManager.runInTransaction(conn -> itemDAO.save(conn, item));
  }

  public Optional<Item> getById(int id) {
    return itemDAO.findById(id);
  }

  public void update(Item item) {
    transactionManager.runInTransaction(
        conn -> {
          itemDAO.update(conn, item);
          return null;
        });
  }

  public void updateStatus(int id, ItemStatus status) {
    transactionManager.runInTransaction(
        conn -> {
          itemDAO
              .findById(conn, id)
              .map(
                  item -> {
                    item.setStatus(status);
                    itemDAO.update(conn, item);
                    return null;
                  });
          return null;
        });
  }

  public void delete(int id) {
    updateStatus(id, ItemStatus.DELETE);
  }

  public List<Item> getAll() {
    return itemDAO.findAll();
  }
}
