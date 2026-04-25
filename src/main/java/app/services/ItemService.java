package app.services;

import app.dao.ItemDAO;
import app.models.Item;
import java.util.List;

public class ItemService {
  private final ItemDAO itemDAO;

  public ItemService() {
    this.itemDAO = new ItemDAO();
  }

  public ItemService(ItemDAO itemDAO) {
    this.itemDAO = itemDAO;
  }

  public Item addItem(Item item) {
    return itemDAO.addItem(item);
  }

  public Item getItemById(int id) {
    return itemDAO.getItemById(id);
  }

  public boolean updateItem(Item item) {
    return itemDAO.updateItem(item);
  }

  public boolean deleteItem(int id) {
    return itemDAO.deleteItem(id);
  }

  public List<Item> getAllItems() {
    return itemDAO.getAllItems();
  }
}
