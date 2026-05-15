package app.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.TestFixtures;
import app.dao.impl.MySqlItemDao;
import app.dao.impl.MySqlUserDao;
import app.enums.ItemStatus;
import app.enums.ItemType;
import app.enums.UserRole;
import app.models.Item;
import app.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MySqlItemDaoTest extends BaseDaoTest {
  private UserDao userDao;
  private ItemDao itemDao;
  private User seller;

  @BeforeEach
  void setUp() {
    userDao = new MySqlUserDao();
    itemDao = new MySqlItemDao();
    seller = userDao.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void save_shouldPersistItemWithAvailableStatus() {
    Item saved = itemDao.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));

    assertTrue(saved.getId() > 0);
    Item found = itemDao.findById(saved.getId()).orElseThrow();
    assertEquals("Laptop", found.getName());
    assertEquals(seller.getId(), found.getSellerId());
    assertEquals(ItemType.ELECTRONICS, found.getType());
    assertEquals(ItemStatus.AVAILABLE, found.getStatus());
  }

  @Test
  void findBySeller_shouldReturnOnlySellerItems() {
    User otherSeller =
        userDao.save(TestFixtures.user(TestFixtures.unique("other_seller"), UserRole.SELLER));
    Item sellerItem =
        itemDao.save(TestFixtures.item(seller.getId(), "Camera", ItemType.ELECTRONICS));
    itemDao.save(TestFixtures.item(otherSeller.getId(), "Painting", ItemType.ART));

    var items = itemDao.findBySeller(seller.getId());

    assertEquals(1, items.size());
    assertEquals(sellerItem.getId(), items.getFirst().getId());
  }

  @Test
  void findByCategoryAndAvailable_shouldFilterItems() {
    Item phone = itemDao.save(TestFixtures.item(seller.getId(), "Phone", ItemType.ELECTRONICS));
    Item painting = itemDao.save(TestFixtures.item(seller.getId(), "Painting", ItemType.ART));
    painting.setStatus(ItemStatus.SOLD);
    itemDao.update(painting);

    var electronics = itemDao.findByCategory(ItemType.ELECTRONICS);
    var available = itemDao.findAvailable();

    assertEquals(1, electronics.size());
    assertEquals(phone.getId(), electronics.getFirst().getId());
    assertEquals(1, available.size());
    assertEquals(phone.getId(), available.getFirst().getId());
  }

  @Test
  void update_shouldPersistEditableFieldsAndStatus() {
    Item saved = itemDao.save(TestFixtures.item(seller.getId(), "Old name", ItemType.ELECTRONICS));
    saved.setName("New name");
    saved.setDescription("Updated description");
    saved.setStartingPrice(2000L);
    saved.setStepPrice(200L);
    saved.setType(ItemType.VEHICLE);
    saved.setStatus(ItemStatus.UNDER_AUCTION);

    itemDao.update(saved);

    Item found = itemDao.findById(saved.getId()).orElseThrow();
    assertEquals("New name", found.getName());
    assertEquals("Updated description", found.getDescription());
    assertEquals(2000L, found.getStartingPrice());
    assertEquals(200L, found.getStepPrice());
    assertEquals(ItemType.VEHICLE, found.getType());
    assertEquals(ItemStatus.UNDER_AUCTION, found.getStatus());
  }
}
