package app.service;

import static org.junit.jupiter.api.Assertions.*;

import app.dao.BaseDAOTest;
import app.dao.UserDAO;
import app.dao.impl.MySqlUserDAO;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import app.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServiceTest extends BaseDAOTest {

  private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);

  private User tester;
  private UserService userService;
  private UserDAO userDAO;

  @BeforeEach
  void setup() {
    logger.info("Setting up UserService test...");

    userDAO = new MySqlUserDAO();
    userService = new UserService(userDAO);

    // ❌ KHÔNG truncate SQL thủ công nữa
    userDAO.deleteAll(); // 👉 bạn cần implement method này

    tester =
        UserFactory.createUser(
            "Test User",
            new Account("test_account", PasswordUtils.hashPassword("test_password")),
            new Wallet(),
            UserRole.BIDDER);

    tester = userDAO.save(tester);

    logger.info("Test user created: {}", tester.getId());
  }

  @Test
  void testLogin_success() {
    User loggedIn = userService.login("test_account", "test_password");

    assertNotNull(loggedIn);
    assertEquals(tester.getId(), loggedIn.getId());
    assertEquals("test_account", loggedIn.getAccount().getUsername());
  }

  @Test
  void testLogin_wrongPassword_shouldThrow() {
    assertThrows(ServiceException.class, () -> userService.login("test_account", "wrong"));
  }

  @Test
  void testLogin_unknownUser_shouldThrow() {
    assertThrows(ServiceException.class, () -> userService.login("unknown", "test_password"));
  }
}
