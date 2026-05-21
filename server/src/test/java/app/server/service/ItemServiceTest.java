package app.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.TestFixtures;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.BaseDAOTest;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.dao.impl.MySqlItemDAO;
import app.server.dao.impl.MySqlUserDAO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemServiceTest extends BaseDAOTest {
  private ItemDAO itemDAO;
  private UserDAO userDAO;
  private ItemService itemService;
  private User seller;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    itemDAO = new MySqlItemDAO();
    itemService = new ItemService(itemDAO);
    seller = userDAO.save(TestFixtures.user(TestFixtures.unique("seller"), UserRole.SELLER));
  }

  @Test
  void getSellerItems_shouldReturnOwnItems() {
    itemDAO.save(TestFixtures.item(seller.getId(), "Laptop", ItemType.ELECTRONICS));

    List<Item> items = itemService.getSellerItems(seller.getId(), seller.getRole(), seller.getId());

    assertEquals(1, items.size());
    assertEquals("Laptop", items.getFirst().getName());
  }

  @Test
  void getSellerItems_shouldAllowAdminToViewSellerItems() {
    User admin = userDAO.save(TestFixtures.user(TestFixtures.unique("admin"), UserRole.ADMIN));
    itemDAO.save(TestFixtures.item(seller.getId(), "Bike", ItemType.VEHICLE));

    List<Item> items = itemService.getSellerItems(admin.getId(), admin.getRole(), seller.getId());

    assertEquals(1, items.size());
    assertEquals("Bike", items.getFirst().getName());
  }

  @Test
  void getSellerItems_shouldRejectOtherSeller() {
    User otherSeller =
        userDAO.save(TestFixtures.user(TestFixtures.unique("other-seller"), UserRole.SELLER));

    assertThrows(
        ServiceException.class,
        () -> itemService.getSellerItems(otherSeller.getId(), otherSeller.getRole(), seller.getId()));
  }
}
