package app.dao;

import static org.junit.jupiter.api.Assertions.*;

import app.config.DatabaseConnection;
import app.dao.impl.MySqlUserDAO;
import app.enums.UserRole;
import app.exception.DatabaseException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UserDAOTest extends BaseDAOTest {

  private static final Logger logger = LoggerFactory.getLogger(UserDAOTest.class);

  private UserDAO userDAO;

  @BeforeEach
  void setUp() {
    userDAO = new MySqlUserDAO();
    cleanDatabase();
  }

  // =========================
  // CLEAN ONLY DATA (no FK toggle)
  // =========================
  private void cleanDatabase() {
    try (var conn = DatabaseConnection.getConnection();
        var stmt = conn.createStatement()) {

      stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

      stmt.execute("TRUNCATE TABLE auto_bids");
      stmt.execute("TRUNCATE TABLE bids");
      stmt.execute("TRUNCATE TABLE auction_sessions");
      stmt.execute("TRUNCATE TABLE items");
      stmt.execute("TRUNCATE TABLE users");

      stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private User makeUser(String username) {
    return UserFactory.createUser(
        0,
        "Nguyen Van A",
        new Account(username, "rawPassword"),
        new Wallet(1_000_000L),
        UserRole.BIDDER);
  }

  // ───── save ─────

  @Test
  void save_shouldPersistUserAndReturnId() {
    User saved = userDAO.save(makeUser("alice"));

    assertTrue(saved.getId() > 0);

    User found = userDAO.findById(saved.getId()).orElseThrow();
    assertEquals("alice", found.getAccount().getUsername());
    assertEquals("Nguyen Van A", found.getName());
    assertEquals(UserRole.BIDDER, found.getRole());
    assertEquals(1_000_000L, found.getWallet().getAssets());
  }

  @Test
  void save_duplicateUsername_shouldThrow() {
    userDAO.save(makeUser("bob"));

    assertThrows(DatabaseException.class, () -> userDAO.save(makeUser("bob")));
  }

  // ───── findById ─────

  @Test
  void findById_shouldReturnUser() {
    User saved = userDAO.save(makeUser("charlie"));

    Optional<User> found = userDAO.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals("charlie", found.get().getAccount().getUsername());
  }

  @Test
  void findById_shouldReturnEmpty() {
    assertTrue(userDAO.findById(99999).isEmpty());
  }

  // ───── findByUsername ─────

  @Test
  void findByUsername_shouldReturnUser() {
    userDAO.save(makeUser("dave"));

    Optional<User> found = userDAO.findByUsername("dave");

    assertTrue(found.isPresent());
    assertEquals("Nguyen Van A", found.get().getName());
  }

  @Test
  void findByUsername_shouldReturnEmpty() {
    assertTrue(userDAO.findByUsername("ghost").isEmpty());
  }
}
