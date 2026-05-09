// src/test/java/app/dao/UserDAOTest.java
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
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UserDAOTest extends BaseDAOTest {

  private static final Logger logger = LoggerFactory.getLogger(UserDAOTest.class);

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
    logger.info("Cleaning up database before test execution...");
    userDAO = new MySqlUserDAO();
    try (Connection conn = DatabaseConnection.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
      stmt.execute("TRUNCATE TABLE bids");
      stmt.execute("TRUNCATE TABLE auto_bids");
      stmt.execute("TRUNCATE TABLE auction_sessions");
      stmt.execute("TRUNCATE TABLE items");
      stmt.execute("TRUNCATE TABLE users");
      stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
      logger.info("Database cleaned up successfully.");
    } catch (Exception e) {
      logger.error("Failed to clean up database", e);
      throw e;
    }
  }

  // ───── save ─────

  @Test
  void save_shouldPersistUserAndReturnGeneratedId() throws Exception {
    logger.info("Running test: save_shouldPersistUserAndReturnGeneratedId");
    try (Connection conn = DatabaseConnection.getConnection()) {
      User saved = userDAO.save(makeUser("alice"));

      assertTrue(saved.getId() > 0);

      User stored = userDAO.findById(saved.getId()).orElseThrow();
      assertEquals("alice", stored.getAccount().getUsername());
      assertEquals("Nguyen Van A", stored.getName());
      assertEquals(UserRole.BIDDER, stored.getRole());
      assertEquals(1_000_000L, stored.getWallet().getAssets());
      System.out.println("Stored password hash: " + stored.getAccount().getPassword());
      assertEquals("rawPassword", stored.getAccount().getPassword());
      logger.info("Test passed: User saved and verified successfully");
    }
  }

  @Test
  void save_duplicateUsername_shouldThrow() throws Exception {
    logger.info("Running test: save_duplicateUsername_shouldThrow");
    userDAO.save(makeUser("bob"));

    assertThrows(DatabaseException.class, () -> userDAO.save(makeUser("bob")));
    logger.info("Test passed: DatabaseException thrown for duplicate username");
  }

  // ───── findById ─────

  @Test
  void findById_existingUser_shouldReturnUser() throws Exception {
    logger.info("Running test: findById_existingUser_shouldReturnUser");
    User saved = userDAO.save(makeUser("charlie"));

    Optional<User> found = userDAO.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals("charlie", found.get().getAccount().getUsername());
    logger.info("Test passed: User found by ID successfully");
  }

  @Test
  void findById_nonExisting_shouldReturnEmpty() throws Exception {
    logger.warn(
        "Running test: findById_nonExisting_shouldReturnEmpty - checking non-existent data");
    try (Connection conn = DatabaseConnection.getConnection()) {
      Optional<User> found = userDAO.findById(99999);

      assertTrue(found.isEmpty());
      logger.info("Test passed: Empty optional returned for non-existent user");
    }
  }

  // ───── findByUsername ─────

  @Test
  void findByUsername_existingUser_shouldReturnUser() throws Exception {
    logger.info("Running test: findByUsername_existingUser_shouldReturnUser");
    userDAO.save(makeUser("dave"));

    Optional<User> found = userDAO.findByUsername("dave");

    assertTrue(found.isPresent());
    assertEquals("Nguyen Van A", found.get().getName());
    logger.info("Test passed: User found by username successfully");
  }

  @Test
  void findByUsername_nonExisting_shouldReturnEmpty() throws Exception {
    logger.warn(
        "Running test: findByUsername_nonExisting_shouldReturnEmpty - checking non-existent data");
    assertTrue(userDAO.findByUsername("ghost").isEmpty());
    logger.info("Test passed: Empty optional returned for non-existent username");
  }
}
