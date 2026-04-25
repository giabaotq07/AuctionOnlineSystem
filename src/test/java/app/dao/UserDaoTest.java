package app.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.config.DatabaseConnection;
import app.exceptions.UserNotFoundException;
import app.models.User;
import app.services.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UserDaoTest {
  static UserService userService;
  static User tester;

  @BeforeAll
  public static void setupDatabase() {
    DatabaseConnection.initializeDatabase();
    userService = new UserService();
    tester = new User(12,"Test User", "test_account", "test_password");
  }

  @Test
  void testGetUserByAccount0() {
    User user = userService.getUserByAccount("test_account");
    assertEquals(user, tester);
  }

  @Test
  void testGetUserByAccount1() {
    assertThrows(UserNotFoundException.class, () -> {
      userService.getUserByAccount("non_existent_account");
    });
  }

}
