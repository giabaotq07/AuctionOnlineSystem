package app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.config.DatabaseConnection;
import app.dao.BaseDAOTest;
import app.dao.UserDAO;
import app.enums.UserRole;
import app.exception.ServiceException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServiceTest extends BaseDAOTest {

  private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);

  private User tester;
  private UserService userService;

  @BeforeEach
  public void setupDatabase() throws Exception {
    logger.info("Initializing UserService test database and mock data...");
    UserDAO userDAO = new UserDAO();
    userService = new UserService(userDAO);

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
      stmt.execute("TRUNCATE TABLE bids");
      stmt.execute("TRUNCATE TABLE auto_bids");
      stmt.execute("TRUNCATE TABLE auction_sessions");
      stmt.execute("TRUNCATE TABLE items");
      stmt.execute("TRUNCATE TABLE users");
      stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
      logger.info("Database truncated successfully.");
    } catch (Exception e) {
      logger.error("Error truncating test database tables", e);
      throw e;
    }

    tester =
        UserFactory.createUser(
            "Test User",
            new Account("test_account", "test_password"),
            new Wallet(),
            UserRole.BIDDER);
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      tester = userDAO.save(conn, tester);
      logger.info("Mock user created and saved.");
    } catch (Exception e) {
      logger.error("Error saving mock user", e);
      throw e;
    }
  }

  @Test
  public void testLogin_success() {
    logger.info("Running test: testLogin_success");
    User loggedIn = userService.login("test_account", "test_password");

    assertNotNull(loggedIn);
    assertEquals(tester.getId(), loggedIn.getId());
    assertEquals("test_account", loggedIn.getAccount().getUsername());
    logger.info("Test passed: Successful login verified");
  }

  @Test
  public void testLogin_wrongPassword_shouldThrow() {
    logger.warn("Running test: testLogin_wrongPassword_shouldThrow - expecting exception");
    assertThrows(ServiceException.class, () -> userService.login("test_account", "wrong"));
    logger.info("Test passed: Exception thrown for wrong password");
  }

  @Test
  public void testLogin_unknownUser_shouldThrow() {
    logger.warn("Running test: testLogin_unknownUser_shouldThrow - expecting exception");
    assertThrows(ServiceException.class, () -> userService.login("unknown", "test_password"));
    logger.info("Test passed: Exception thrown for unknown user");
  }
}
