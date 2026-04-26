package app.dao;

import static org.junit.jupiter.api.Assertions.*;

import app.config.DatabaseConnection;
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
            UserRole.BIDDER.name());
    tester = userDAO.addUser(tester);
  }

  @Test
  void testGetUserByAccount() {
    User user = userDAO.getUserByAccount("test_account");
    assertEquals(user, tester);
    user = userDAO.getUserByAccount("non_existent_account");
    assertNull(user);
  }

  @Test
  void testAddUser() {
    User user =
        UserFactory.createUser(
            "New User",
            new Account("new_account", "new_password"),
            new Wallet(),
            UserRole.BIDDER.name());
    user = userDAO.addUser(user);
    User addedUser =
        UserFactory.createUser(
            "New User",
            new Account("new_account", "new_password"),
            new Wallet(),
            UserRole.BIDDER.name());
    assertThrows(
        DatabaseException.class,
        () -> userDAO.addUser(addedUser)); // Thử thêm lại để kiểm tra trùng account
    assertNotNull(user);
    assertTrue(user.getId() > 0);
    assertEquals("new_account", user.getAccount().getUsername());

    // Clean up
    userDAO.deleteUser(user.getId());
  }

  @Test
  void testDeleteUser() {
    User user =
        UserFactory.createUser(
            "New User",
            new Account("new_account", "new_password"),
            new Wallet(),
            UserRole.BIDDER.name());
    user = userDAO.addUser(user);
    assertTrue(userDAO.deleteUser(user.getId()));
    assertFalse(userDAO.deleteUser(user.getId())); // Thử xóa lại để kiểm tra đã xóa
  }

  @Test
  void testUpdateUser() {
    User user =
        UserFactory.createUser(
            "New User",
            new Account("new_account", "new_password"),
            new Wallet(),
            UserRole.BIDDER.name());
    user = userDAO.addUser(user);
    user.setName("Updated Name");
    assertTrue(userDAO.updateUserProfile(user));
    User updatedUser = userDAO.getUserByAccount("new_account");
    assertEquals(user.getName(), updatedUser.getName());
    userDAO.deleteUser(user.getId());
  }
}
