package app.service;

import app.dao.ItemDAO;
import app.enums.ItemStatus;
import app.models.Item;
import java.util.List;

public class ItemService {
  private ItemDAO itemDAO;

  public ItemService(ItemDAO itemDAO) {
    this.itemDAO = itemDAO;
  }

  public Item add(Item item) {
    return itemDAO.save(item);
  }

  public Item getById(int id) {
    return itemDAO.findById(id).orElse(null);
  }

  public void update(Item item) {
    itemDAO.update(item);
  }

  public void updateStatus(int id, ItemStatus status) {
    itemDAO.updateStatus(id, status);
  }

  public void delete(int id) {
    itemDAO.updateStatus(id, ItemStatus.DELETE);
  }

  public List<Item> getAll() {
    return itemDAO.findAll();
  }
}
