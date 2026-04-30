package app.service;

import app.dao.ItemDAO;
import app.models.Item;
import java.util.List;

public class ItemService {
  private final ItemDAO itemDAO;

  public ItemService(ItemDAO itemDAO) {
    this.itemDAO = itemDAO;
  }

  public Item add(Item item) {
    return itemDAO.add(item);
  }

  public Item getById(int id) {
    return itemDAO.getById(id);
  }

  public boolean update(Item item) {
    return itemDAO.update(item);
  }

  public boolean delete(int id) {
    return itemDAO.delete(id);
  }

  public List<Item> getAll() {
    return itemDAO.getAll();
  }
}
