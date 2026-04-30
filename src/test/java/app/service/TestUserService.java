package app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.config.DatabaseConnection;
import app.dao.UserDAO;
import app.models.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestUserService {
  static User tester;
  static UserDAO userDAO;
  static UserService userService;

  @BeforeAll
  public static void setupDatabase() {
    DatabaseConnection.initializeDatabase();
    userDAO = UserDAO.getInstance();
    userService = new UserService(userDAO);
    tester = userService.getUserByAccount("test_account");
  }

  @Test
  public void testLogin() {
    User logined = userService.login("test_account", "test_password");
    assertEquals(tester, logined);
  }
}
