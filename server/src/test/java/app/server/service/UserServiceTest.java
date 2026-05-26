package app.server.service;

import static org.junit.jupiter.api.Assertions.*;

import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Account;
import app.common.models.User;
import app.common.models.Wallet;
import app.server.dao.BaseDAOTest;
import app.server.dao.UserDAO;
import app.server.dao.impl.MySqlUserDAO;
import app.server.database.TransactionManager;
import app.server.utils.PasswordUtils;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServiceTest extends BaseDAOTest {
  private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);
  private UserService userService;
  private UserDAO userDAO;
  private TransactionManager transactionManager;
  private User tester;

  // =========================
  // SETUP
  // =========================
  @BeforeEach
  void setup() {
    logger.info("Setting up UserService test...");
    userDAO = new MySqlUserDAO();
    transactionManager = new TransactionManager();

    userService = new UserService(userDAO, transactionManager);
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
  void ban_user_shouldSetStatusToFalse() {
    userService.banUser(tester.getId());

    User banned = userDAO.findById(tester.getId()).orElseThrow();
    assertTrue(banned.isBanned());
  }

  @Test
  void unban_user_shouldSetStatusToTrue() {
    userService.banUser(tester.getId());
    userService.unbanUser(tester.getId());

    User unbanned = userDAO.findById(tester.getId()).orElseThrow();
    assertFalse(unbanned.isBanned());
  }

  @Test
  void login_shouldFail_whenPasswordWrong() {
    assertThrows(ServiceException.class, () -> userService.login("test_account", "wrong_password"));
  }

  @Test
  void login_shouldFail_whenUserNotFound() {
    assertThrows(ServiceException.class, () -> userService.login("unknown_user", "test_password"));
  }

  @Test
  void login_shouldFail_whenUserIsBanned() {
    userService.banUser(tester.getId());

    ServiceException exception =
        assertThrows(
            ServiceException.class, () -> userService.login("test_account", "test_password"));
    assertEquals("Tài khoản đã bị cấm.", exception.getMessage());
  }

  @Test
  void register_shouldHashPasswordAndPersistUser() {
    User rawUser =
        new User(
            "Registered User",
            new Account("registered_user", "plain_password", UserRole.BIDDER),
            new Wallet());

    User saved = userService.register(rawUser);

    User stored = userDAO.findById(saved.getId()).orElseThrow();
    assertEquals("registered_user", stored.getAccount().getUsername());
    assertNotEquals("plain_password", stored.getAccount().getPassword());
    assertTrue(PasswordUtils.verify("plain_password", stored.getAccount().getPassword()));
  }

  @Test
  void register_shouldFail_whenUsernameAlreadyExists() {
    User duplicate =
        new User(
            "Duplicate User",
            new Account("test_account", "plain_password", UserRole.BIDDER),
            new Wallet());

    assertThrows(ServiceException.class, () -> userService.register(duplicate));
  }

  @Test
  void deposit_shouldIncreaseAvailableBalance() {
    User updated = userService.deposit(tester.getId(), new BigDecimal("50000"));

    assertEquals(0, updated.getWallet().getAvailableBalance().compareTo(new BigDecimal("50000")));
    User stored = userDAO.findById(tester.getId()).orElseThrow();
    assertEquals(0, stored.getWallet().getAvailableBalance().compareTo(new BigDecimal("50000")));
  }

  @Test
  void deposit_shouldFail_whenAmountIsInvalid() {
    assertThrows(
        ServiceException.class, () -> userService.deposit(tester.getId(), BigDecimal.ZERO));
  }

  @Test
  void changePassword_shouldPersistNewHashedPassword() {
    userService.changePassword("test_account", "test_password", "new_password");

    User result = userService.login("test_account", "new_password");
    assertEquals(tester.getId(), result.getId());
    assertThrows(ServiceException.class, () -> userService.login("test_account", "test_password"));
  }

  @Test
  void getById_shouldReturnUser_whenExists() {
    User found = userService.getById(tester.getId());
    assertNotNull(found);
    assertEquals(tester.getId(), found.getId());
  }

  @Test
  void getById_shouldThrow_whenNotFound() {
    assertThrows(ServiceException.class, () -> userService.getById(-999));
  }

  @Test
  void getAllUsers_shouldReturnList() {
    var users = userService.getAllUsers(tester.getId());
    assertNotNull(users);
    assertFalse(users.isEmpty());
  }

  @Test
  void updateProfile_shouldUpdateName() {
    tester.setName("Updated Name");
    userService.updateProfile(tester);

    User stored = userDAO.findById(tester.getId()).orElseThrow();
    assertEquals("Updated Name", stored.getName());
  }

  @Test
  void updateAvatarUrl_shouldPersistUrl() {
    String oldUrl = userService.updateAvatarUrl(tester.getId(), "server_data/images/avatar.png");
    assertNull(oldUrl); // User chua co avatar truoc do

    User stored = userDAO.findById(tester.getId()).orElseThrow();
    assertEquals("server_data/images/avatar.png", stored.getAvatarUrl());
  }

  @Test
  void banUser_shouldClearFrozenFundsAndBan() {
    // Nap tien truoc de co du so du cho setFrozenAmount
    tester = userDAO.findById(tester.getId()).orElseThrow();
    tester.getWallet().deposit(java.math.BigDecimal.valueOf(500));
    userDAO.update(tester);
    // Dat frozen funds truoc khi ban
    tester = userDAO.findById(tester.getId()).orElseThrow();
    tester.getWallet().setFrozenAmount("auction_1", java.math.BigDecimal.valueOf(100));
    userDAO.update(tester);

    userService.banUser(tester.getId());

    User banned = userDAO.findById(tester.getId()).orElseThrow();
    assertTrue(banned.isBanned());
    assertTrue(banned.getWallet().getFrozenFundsSnapshot().isEmpty());
  }

  // =========================
  // HELPERS
  // =========================
  private User createTestUser() {
    return new User(
        "Test User",
        new Account("test_account", PasswordUtils.hashPassword("test_password"), UserRole.BIDDER),
        new Wallet());
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
