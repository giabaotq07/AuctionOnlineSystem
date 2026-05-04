// src/test/java/app/dao/UserDAOTest.java
package app.dao;

import static org.junit.jupiter.api.Assertions.*;

import app.config.DatabaseConnection;
import app.enums.UserRole;
import app.exception.DatabaseException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import app.util.PasswordUtils;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;
import org.junit.jupiter.api.*;

class UserDAOTest extends BaseDAOTest {

  private UserDAO userDAO;

  private User makeUser(String username) {
    return UserFactory.createUser(
        0,
        "Nguyen Van A",
        new Account(username, "rawPassword"),
        new Wallet(1_000_000L),
        UserRole.BIDDER);
  }

  @BeforeEach
  void cleanUp() throws Exception {
    userDAO = new UserDAO();
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
  }

  // ───── save ─────

  @Test
  void save_shouldPersistUserAndReturnGeneratedId() {
    User saved = userDAO.save(makeUser("alice"));

    assertTrue(saved.getId() > 0);

    User stored = userDAO.findById(saved.getId()).orElseThrow();
    assertEquals("alice", stored.getAccount().getUsername());
    assertEquals("Nguyen Van A", stored.getName());
    assertEquals(UserRole.BIDDER, stored.getRole());
    assertEquals(1_000_000L, stored.getWallet().getAssets());
    assertNotEquals("rawPassword", stored.getAccount().getPassword());
    assertTrue(PasswordUtils.verify("rawPassword", stored.getAccount().getPassword()));
  }

  @Test
  void save_duplicateUsername_shouldThrow() {
    userDAO.save(makeUser("bob"));

    assertThrows(DatabaseException.class, () -> userDAO.save(makeUser("bob")));
  }

  // ───── findById ─────

  @Test
  void findById_existingUser_shouldReturnUser() {
    User saved = userDAO.save(makeUser("charlie"));

    Optional<User> found = userDAO.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals("charlie", found.get().getAccount().getUsername());
  }

  @Test
  void findById_nonExisting_shouldReturnEmpty() {
    Optional<User> found = userDAO.findById(99999);

    assertTrue(found.isEmpty());
  }

  // ───── findByUsername ─────

  @Test
  void findByUsername_existingUser_shouldReturnUser() {
    userDAO.save(makeUser("dave"));

    Optional<User> found = userDAO.findByUsername("dave");

    assertTrue(found.isPresent());
    assertEquals("Nguyen Van A", found.get().getName());
  }

  @Test
  void findByUsername_nonExisting_shouldReturnEmpty() {
    assertTrue(userDAO.findByUsername("ghost").isEmpty());
  }
}
