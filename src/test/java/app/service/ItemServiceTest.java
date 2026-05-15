package app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.dao.AuctionDao;
import app.dao.BaseDaoTest;
import app.dao.ItemDao;
import app.dao.UserDao;
import app.dao.impl.MySqlAuctionDao;
import app.dao.impl.MySqlItemDao;
import app.dao.impl.MySqlUserDao;
import app.database.TransactionManager;
import app.enums.ItemStatus;
import app.enums.ItemType;
import app.enums.UserRole;
import app.models.Item;
import app.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemServiceTest extends BaseDaoTest {
  private ItemDao itemDao;
  private ItemService itemService;
  private User seller;

  @BeforeEach
  void setUp() {
    UserDao userDao = new MySqlUserDao();
    itemDao = new MySqlItemDao();
    AuctionDao auctionDao = new MySqlAuctionDao();
    itemService = new ItemService(itemDao, auctionDao, new TransactionManager());
    seller = userDao.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void add_shouldPersistItem() {
    Item saved = itemService.add(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));

    assertTrue(saved.getId() > 0);
    Item found = itemService.getById(saved.getId()).orElseThrow();
    assertEquals("Laptop", found.getName());
    assertEquals(ItemStatus.AVAILABLE, found.getStatus());
  }

  @Test
  void update_shouldPersistItemChanges() {
    Item saved = itemService.add(TestFixtures.item(seller.getId(), "Old name", ItemType.ART));
    saved.setName("New name");
    saved.setDescription("New description");
    saved.setStartingPrice(2000L);
    saved.setStepPrice(250L);

    itemService.update(saved);

    Item found = itemService.getById(saved.getId()).orElseThrow();
    assertEquals("New name", found.getName());
    assertEquals("New description", found.getDescription());
    assertEquals(2000L, found.getStartingPrice());
    assertEquals(250L, found.getStepPrice());
  }

  @Test
  void updateStatus_shouldPersistStatus() {
    Item saved = itemService.add(TestFixtures.item(seller.getId(), "Painting", ItemType.ART));

    itemService.updateStatus(saved.getId(), ItemStatus.SOLD);

    Item found = itemDao.findById(saved.getId()).orElseThrow();
    assertEquals(ItemStatus.SOLD, found.getStatus());
  }

  @Test
  void delete_shouldMarkItemAsDeleted() {
    Item saved = itemService.add(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));

    itemService.delete(saved.getId());

    Item found = itemDao.findById(saved.getId()).orElseThrow();
    assertEquals(ItemStatus.DELETE, found.getStatus());
  }

  @Test
  void getAll_shouldReturnPersistedItems() {
    itemService.add(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    itemService.add(TestFixtures.item(seller.getId(), "Bike", ItemType.VEHICLE));

    assertEquals(2, itemService.getAll().size());
  }
}
