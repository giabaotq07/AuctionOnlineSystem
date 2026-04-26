package app.dao;

import static org.junit.jupiter.api.Assertions.*;

import app.config.DatabaseConnection;
import app.exceptions.DatabaseException;
import app.models.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestUserDao {
  static User tester;
  static UserDAO userDAO;

  @BeforeAll
  public static void setupDatabase() {
    DatabaseConnection.initializeDatabase();
    userDAO = new UserDAO();
    tester = new User(12, "Test User", "test_account", "test_password");
  }

  @Test
  void testGetUserByAccount() {
    User user = userDAO.loadUsers("test_account");
    assertEquals(user, tester);
    user = userDAO.loadUsers("non_existent_account");
    assertNull(user);
  }

  @Test
  void testAddUser() {
    User newUser = new User(0, "New User", "new_account", "new_password");
    User addedUser = userDAO.addUser(newUser);
    assertThrows(DatabaseException.class, () -> userDAO.addUser(newUser)); // Thử thêm lại để kiểm tra trùng account
    assertNotNull(addedUser);
    assertTrue(addedUser.getId() > 0);
    assertEquals("new_account", addedUser.getAccount());

    // Clean up
    userDAO.deleteUser("new_account");
  }

  @Test
  void testDeleteUser() {
    User newUser = new User(0, "New User", "new_account", "new_password");
    User addedUser = userDAO.addUser(newUser);
    assertTrue(userDAO.deleteUser("new_account"));
    assertFalse(userDAO.deleteUser("non_existent_account"));
  }

  @Test
  void testUpdateUser() {
    User newUser = new User(0, "New User", "new_account", "new_password");
    User addedUser = userDAO.addUser(newUser);
    addedUser.setName("Updated Name");
    assertTrue(userDAO.updateUser(addedUser));
    User updatedUser = userDAO.loadUsers("new_account");
    assertEquals(newUser.getName(), updatedUser.getName());
    userDAO.deleteUser("new_account");
  }
}
