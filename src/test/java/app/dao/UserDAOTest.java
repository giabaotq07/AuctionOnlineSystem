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
import app.utils.PasswordUtils;
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
  void save_shouldPersistUserAndReturnGeneratedId() throws Exception {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      User saved = userDAO.save(conn, makeUser("alice"));

      assertTrue(saved.getId() > 0);

      User stored = userDAO.findById(conn, saved.getId()).orElseThrow();
      assertEquals("alice", stored.getAccount().getUsername());
      assertEquals("Nguyen Van A", stored.getName());
      assertEquals(UserRole.BIDDER, stored.getRole());
      assertEquals(1_000_000L, stored.getWallet().getAssets());
      assertNotEquals("rawPassword", stored.getAccount().getPassword());
      assertTrue(PasswordUtils.verify("rawPassword", stored.getAccount().getPassword()));
    }
  }

  @Test
  void save_duplicateUsername_shouldThrow() throws Exception {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      userDAO.save(conn, makeUser("bob"));

      assertThrows(DatabaseException.class, () -> userDAO.save(conn, makeUser("bob")));
    }
  }

  // ───── findById ─────

  @Test
  void findById_existingUser_shouldReturnUser() throws Exception {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      User saved = userDAO.save(conn, makeUser("charlie"));

      Optional<User> found = userDAO.findById(conn, saved.getId());

      assertTrue(found.isPresent());
      assertEquals("charlie", found.get().getAccount().getUsername());
    }
  }

  @Test
  void findById_nonExisting_shouldReturnEmpty() throws Exception {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      Optional<User> found = userDAO.findById(conn, 99999);

      assertTrue(found.isEmpty());
    }
  }

  // ───── findByUsername ─────

  @Test
  void findByUsername_existingUser_shouldReturnUser() throws Exception {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      userDAO.save(conn, makeUser("dave"));

      Optional<User> found = userDAO.findByUsername(conn, "dave");

      assertTrue(found.isPresent());
      assertEquals("Nguyen Van A", found.get().getName());
    }
  }

  @Test
  void findByUsername_nonExisting_shouldReturnEmpty() throws Exception {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      assertTrue(userDAO.findByUsername(conn, "ghost").isEmpty());
    }
  }
}
