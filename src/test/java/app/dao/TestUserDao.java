package app.dao;

import static org.junit.jupiter.api.Assertions.*;

import app.enums.UserRole;
import app.exception.DatabaseException;
import app.models.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestUserDao {
  static User tester;
  static UserDAO userDAO;

  @BeforeAll
  public static void setupDatabase() {
    userDAO = new UserDAO();
    tester =
        UserFactory.createUser(
            "Test User",
            new Account("test_account", "test_password"),
            new Wallet(),
            UserRole.BIDDER);
    try {
      tester = userDAO.save(tester);
    } catch (DatabaseException e) {
      System.err.println("Error setting up test user: " + e.getMessage());
      tester = userDAO.findByUsername("test_account").orElse(null);
    }
  }

  @Test
  void testGetUserByAccount() {
    User user = userDAO.findByUsername("test_account").orElse(null);
    assertEquals(user, tester);
    user = userDAO.findByUsername("non_existent_account").orElse(null);
    assertNull(user);
  }

  @Test
  void testadd() {
    User user =
        UserFactory.createUser(
            "New User", new Account("new_account", "new_password"), new Wallet(), UserRole.BIDDER);
    user = userDAO.save(user);
    User addedUser =
        UserFactory.createUser(
            "New User", new Account("new_account", "new_password"), new Wallet(), UserRole.BIDDER);
    assertThrows(
        DatabaseException.class,
        () -> userDAO.save(addedUser)); // Thử thêm lại để kiểm tra trùng account
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
    user = userDAO.save(user);
    assertTrue(userDAO.delete(user.getId()));
    assertFalse(userDAO.delete(user.getId())); // Thử xóa lại để kiểm tra đã xóa
  }

  @Test
  void testUpdateUser() {
    User user =
        UserFactory.createUser(
            "New User", new Account("new_account", "new_password"), new Wallet(), UserRole.BIDDER);
    user = userDAO.save(user);
    user.setName("Updated Name");
    assertTrue(userDAO.updateProfile(user.getId(), user.getName()));
    User updatedUser = userDAO.findByUsername("new_account").orElse(null);
    assertEquals(user.getName(), updatedUser.getName());
    userDAO.delete(user.getId());
  }
}
