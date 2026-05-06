package app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.config.DatabaseConnection;
import app.dao.BaseDAOTest;
import app.dao.UserDAO;
import app.dao.impl.MySqlUserDAO;
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

public class UserServiceTest extends BaseDAOTest {
  private User tester;
  private UserService userService;

  @BeforeEach
  public void setupDatabase() throws Exception {
    UserDAO userDAO = new MySqlUserDAO();
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
    }

    tester =
        UserFactory.createUser(
            "Test User",
            new Account("test_account", "test_password"),
            new Wallet(),
            UserRole.BIDDER);
    // register via service so password is hashed consistently
    tester = userService.register(tester);
  }

  @Test
  public void testLogin_success() {
    User loggedIn = userService.login("test_account", "test_password");

    assertNotNull(loggedIn);
    assertEquals(tester.getId(), loggedIn.getId());
    assertEquals("test_account", loggedIn.getAccount().getUsername());
  }

  @Test
  public void testLogin_wrongPassword_shouldThrow() {
    assertThrows(ServiceException.class, () -> userService.login("test_account", "wrong"));
  }

  @Test
  public void testLogin_unknownUser_shouldThrow() {
    assertThrows(ServiceException.class, () -> userService.login("unknown", "test_password"));
  }
}
