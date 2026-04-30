package app.dao;

import static org.junit.jupiter.api.Assertions.*;

import app.config.DatabaseConnection;
import app.enums.UserRole;
import app.exceptions.DatabaseException;
import app.models.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestUserDao {
  static User tester;
  static UserDAO userDAO;

  @BeforeAll
  public static void setupDatabase() {
    DatabaseConnection.initializeDatabase();
    userDAO = new UserDAO();
    tester =
        UserFactory.createUser(
            "Test User",
            new Account("test_account", "test_password"),
            new Wallet(),
            UserRole.BIDDER);
    tester = userDAO.add(tester);
  }

  @Test
  void testGetUserByAccount() {
    User user = userDAO.getUserByAccount("test_account");
    assertEquals(user, tester);
    user = userDAO.getUserByAccount("non_existent_account");
    assertNull(user);
  }

  @Test
  void testadd() {
    User user =
        UserFactory.createUser(
            "New User", new Account("new_account", "new_password"), new Wallet(), UserRole.BIDDER);
    user = userDAO.add(user);
    User addedUser =
        UserFactory.createUser(
            "New User", new Account("new_account", "new_password"), new Wallet(), UserRole.BIDDER);
    assertThrows(
        DatabaseException.class,
        () -> userDAO.add(addedUser)); // Thử thêm lại để kiểm tra trùng account
    assertNotNull(user);
    assertTrue(user.getId() > 0);
    assertEquals("new_account", user.getAccount().getUsername());

    // Clean up
    userDAO.delete(user.getId());
  }

  @Test
  void testdelete() {
    User user =
        UserFactory.createUser(
            "New User", new Account("new_account", "new_password"), new Wallet(), UserRole.BIDDER);
    user = userDAO.add(user);
    assertTrue(userDAO.delete(user.getId()));
    assertFalse(userDAO.delete(user.getId())); // Thử xóa lại để kiểm tra đã xóa
  }

  @Test
  void testUpdateUser() {
    User user =
        UserFactory.createUser(
            "New User", new Account("new_account", "new_password"), new Wallet(), UserRole.BIDDER);
    user = userDAO.add(user);
    user.setName("Updated Name");
    assertTrue(userDAO.updateUserProfile(user));
    User updatedUser = userDAO.getUserByAccount("new_account");
    assertEquals(user.getName(), updatedUser.getName());
    userDAO.delete(user.getId());
  }
}
