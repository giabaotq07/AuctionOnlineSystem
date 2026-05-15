package app.dao;

import static org.junit.jupiter.api.Assertions.*;

import app.dao.impl.MySqlUserDao;
import app.database.DatabaseConnection;
import app.enums.UserRole;
import app.exception.DatabaseException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserDaoTest extends BaseDaoTest {
  private UserDao userDao;

  @BeforeEach
  void setUp() {
    userDao = new MySqlUserDao();
    cleanDatabase();
  }

  // =========================
  // CLEAN ONLY DATA (no FK toggle)
  // =========================
  private void cleanDatabase() {
    try (var conn = DatabaseConnection.getDataSource().getConnection();
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
        new Wallet(new BigDecimal("1000000")),
        UserRole.BIDDER);
  }

  // ───── save ─────
  @Test
  void save_shouldPersistUserAndReturnId() {
    User saved = userDao.save(makeUser("alice"));
    assertTrue(saved.getId() > 0);
    User found = userDao.findById(saved.getId()).orElseThrow();
    assertEquals("alice", found.getAccount().getUsername());
    assertEquals("Nguyen Van A", found.getName());
    assertEquals(UserRole.BIDDER, found.getRole());
    assertEquals(0, found.getWallet().getAvailableBalance().compareTo(new BigDecimal("1000000")));
  }

  @Test
  void save_duplicateUsername_shouldThrow() {
    userDao.save(makeUser("bob"));
    assertThrows(DatabaseException.class, () -> userDao.save(makeUser("bob")));
  }

  // ───── findById ─────
  @Test
  void findById_shouldReturnUser() {
    User saved = userDao.save(makeUser("charlie"));
    Optional<User> found = userDao.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("charlie", found.get().getAccount().getUsername());
  }

  @Test
  void findById_shouldReturnEmpty() {
    assertTrue(userDao.findById(99999).isEmpty());
  }

  // ───── findByUsername ─────
  @Test
  void findByUsername_shouldReturnUser() {
    userDao.save(makeUser("dave"));
    Optional<User> found = userDao.findByUsername("dave");
    assertTrue(found.isPresent());
    assertEquals("Nguyen Van A", found.get().getName());
  }

  @Test
  void findByUsername_shouldReturnEmpty() {
    assertTrue(userDao.findByUsername("ghost").isEmpty());
  }
}
