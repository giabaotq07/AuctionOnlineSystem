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
  private UserService userService;
  private UserDAO userDAO;
  private User tester;

  // =========================
  // SETUP
  // =========================
  @BeforeEach
  void setup() {
    logger.info("Setting up UserService test...");
    userDAO = new MySqlUserDAO();
    userService = new UserService(userDAO);
    cleanDatabase();
    tester = createTestUser();
    tester = userDAO.save(tester);
    logger.info("Test user ready: id={}", tester.getId());
  }

  // =========================
  // TEST CASES
  // =========================
  @Test
  void login_shouldSucceed_whenCredentialsCorrect() {
    // act
    User result = userService.login("test_account", "test_password");
    // assert
    assertNotNull(result);
    assertEquals(tester.getId(), result.getId());
    assertEquals("test_account", result.getAccount().getUsername());
  }

  @Test
  void login_shouldFail_whenPasswordWrong() {
    assertThrows(ServiceException.class, () -> userService.login("test_account", "wrong_password"));
  }

  @Test
  void login_shouldFail_whenUserNotFound() {
    assertThrows(ServiceException.class, () -> userService.login("unknown_user", "test_password"));
  }

  // =========================
  // HELPERS
  // =========================
  private User createTestUser() {
    return UserFactory.createUser(
        "Test User",
        new Account("test_account", PasswordUtils.hashPassword("test_password")),
        new Wallet(),
        UserRole.BIDDER);
  }

  /**
   * IMPORTANT: không dùng truncate SQL nữa → dùng DAO abstraction (clean hơn, test independent DB
   * engine)
   */
  private void cleanDatabase() {
    logger.info("Cleaning test data...");
    try {
      userDAO.deleteAll();
    } catch (Exception e) {
      throw new RuntimeException("Failed to clean database", e);
    }
  }
}
