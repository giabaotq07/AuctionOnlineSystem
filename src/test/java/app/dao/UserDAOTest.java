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
import java.util.List;
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

  // ───── findAll ─────

  @Test
  void findAll_shouldReturnAllUsers() {
    userDAO.save(makeUser("user1"));
    userDAO.save(makeUser("user2"));

    List<User> users = userDAO.findAll();

    assertEquals(2, users.size());
    assertTrue(users.stream().anyMatch(u -> u.getAccount().getUsername().equals("user1")));
    assertTrue(users.stream().anyMatch(u -> u.getAccount().getUsername().equals("user2")));
  }

  // ───── updateProfile ─────

  @Test
  void updateProfile_shouldChangeFullName() {
    User saved = userDAO.save(makeUser("eve"));

    userDAO.updateProfile(saved.getId(), "Tran Thi B");

    User updated = userDAO.findById(saved.getId()).orElseThrow();
    assertEquals("Tran Thi B", updated.getName());
  }

  // ───── updateUsername ─────

  @Test
  void updateUsername_shouldPersistNewUsername() {
    User saved = userDAO.save(makeUser("evelyn"));

    userDAO.updateUsername(saved.getId(), "evelyn2");

    User updated = userDAO.findById(saved.getId()).orElseThrow();
    assertEquals("evelyn2", updated.getAccount().getUsername());
  }

  @Test
  void updateUsername_duplicateUsername_shouldThrow() {
    userDAO.save(makeUser("jack"));
    User saved = userDAO.save(makeUser("kate"));

    assertThrows(DatabaseException.class, () -> userDAO.updateUsername(saved.getId(), "jack"));
  }

  // ───── updatePassword ─────

  @Test
  void updatePassword_shouldHashAndPersist() {
    User saved = userDAO.save(makeUser("frank"));

    String oldHash = userDAO.findById(saved.getId()).orElseThrow().getAccount().getPassword();
    userDAO.updatePassword(saved.getId(), "newPassword123");

    User updated = userDAO.findById(saved.getId()).orElseThrow();
    assertNotEquals(oldHash, updated.getAccount().getPassword());
    assertTrue(PasswordUtils.verify("newPassword123", updated.getAccount().getPassword()));
  }

  // ───── adjustWallet ─────

  @Test
  void adjustWallet_deposit_shouldIncreaseAssets() {
    User saved = userDAO.save(makeUser("grace"));

    userDAO.adjustWallet(saved.getId(), 500_000L);

    User updated = userDAO.findById(saved.getId()).orElseThrow();
    assertEquals(1_500_000L, updated.getWallet().getAssets());
  }

  @Test
  void adjustWallet_withdraw_sufficientBalance_shouldDecreaseAssets() {
    User saved = userDAO.save(makeUser("henry"));

    userDAO.adjustWallet(saved.getId(), -400_000L);

    User updated = userDAO.findById(saved.getId()).orElseThrow();
    assertEquals(600_000L, updated.getWallet().getAssets());
  }

  @Test
  void adjustWallet_withdraw_insufficientBalance_shouldThrow() {
    User saved = userDAO.save(makeUser("ivy"));

    assertThrows(DatabaseException.class, () -> userDAO.adjustWallet(saved.getId(), -9_999_999L));
  }
}
