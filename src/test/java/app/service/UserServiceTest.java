package app.service;

import static org.junit.jupiter.api.Assertions.*;

import app.dao.BaseDaoTest;
import app.dao.UserDao;
import app.dao.impl.MySqlUserDao;
import app.database.TransactionManager;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import app.utils.PasswordUtils;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServiceTest extends BaseDaoTest {
  private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);
  private UserService userService;
  private UserDao userDao;
  private TransactionManager transactionManager;
  private User tester;

  // =========================
  // SETUP
  // =========================
  @BeforeEach
  void setup() {
    logger.info("Setting up UserService test...");
    userDao = new MySqlUserDao();
    transactionManager = new TransactionManager();

    userService = new UserService(userDao, transactionManager);
    cleanDatabase();
    tester = createTestUser();
    tester = userDao.save(tester);
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

  @Test
  void register_shouldHashPasswordAndPersistUser() {
    User rawUser =
        UserFactory.createUser(
            "Registered User",
            new Account("registered_user", "plain_password"),
            new Wallet(),
            UserRole.BIDDER);

    User saved = userService.register(rawUser);

    User stored = userDao.findById(saved.getId()).orElseThrow();
    assertEquals("registered_user", stored.getAccount().getUsername());
    assertNotEquals("plain_password", stored.getAccount().getPassword());
    assertTrue(PasswordUtils.verify("plain_password", stored.getAccount().getPassword()));
  }

  @Test
  void register_shouldFail_whenUsernameAlreadyExists() {
    User duplicate =
        UserFactory.createUser(
            "Duplicate User",
            new Account("test_account", "plain_password"),
            new Wallet(),
            UserRole.BIDDER);

    assertThrows(ServiceException.class, () -> userService.register(duplicate));
  }

  @Test
  void deposit_shouldIncreaseAvailableBalance() {
    User updated = userService.deposit(tester.getId(), new BigDecimal("50000"));

    assertEquals(0, updated.getWallet().getAvailableBalance().compareTo(new BigDecimal("50000")));
    User stored = userDao.findById(tester.getId()).orElseThrow();
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
  void reserveAndSettleFrozenAmount_shouldUpdateWallet() {
    userService.deposit(tester.getId(), new BigDecimal("1000"));

    BigDecimal previous = userService.reserveBidAmount(tester.getId(), 10, new BigDecimal("300"));

    assertEquals(0, previous.compareTo(BigDecimal.ZERO));
    User reserved = userDao.findById(tester.getId()).orElseThrow();
    assertEquals(0, reserved.getWallet().getAvailableBalance().compareTo(new BigDecimal("700")));
    assertEquals(0, reserved.getWallet().getFrozenAmount("10").compareTo(new BigDecimal("300")));

    User settled = userService.settleFrozenAmount(tester.getId(), 10, false);

    assertEquals(0, settled.getWallet().getAvailableBalance().compareTo(new BigDecimal("1000")));
    assertEquals(0, settled.getWallet().getFrozenAmount("10").compareTo(BigDecimal.ZERO));
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
      userDao.deleteAll();
    } catch (Exception e) {
      throw new RuntimeException("Failed to clean database", e);
    }
  }
}
