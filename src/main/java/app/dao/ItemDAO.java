package app.dao;

import app.enums.ItemType;
import app.models.Item;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ItemDAO {
  Optional<Item> findById(int id);

  Optional<Item> findById(Connection conn, int id);

  List<Item> findAll();

  List<Item> findBySeller(int sellerId);

  List<Item> findBySeller(Connection conn, int sellerId);

  List<Item> findByCategory(ItemType type);

  List<Item> findAvailable();

  Item save(Item item);

  Item save(Connection conn, Item item);

  void update(Item item);

  void update(Connection conn, Item item);
}
