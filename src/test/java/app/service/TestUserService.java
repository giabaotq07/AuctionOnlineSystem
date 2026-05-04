package app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.dao.UserDAO;
import app.enums.UserRole;
import app.exception.AuthenticationException;
import app.exception.UserAlreadyExistsException;
import app.models.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestUserService {
  static User tester;
  static UserDAO userDAO;
  static UserService userService;

  @BeforeAll
  public static void setupDatabase() {
    userDAO = new UserDAO();
    userService = new UserService(userDAO);
    tester =
        UserFactory.createUser(
            "Test User",
            new Account("test_account", "test_password"),
            new Wallet(),
            UserRole.BIDDER);
    try {
      tester = userDAO.save(tester);
    } catch (UserAlreadyExistsException e) {
      System.err.println("Error inserting test user: " + e.getMessage());
      tester = userDAO.findByUsername("test_account").orElse(null);
    }
  }

  @Test
  public void testLogin() {
    User logined;
    try {
      logined = userService.login("test_account", "test_password");
    } catch (AuthenticationException e) {
      System.err.println("Login failed: " + e.getMessage());
      logined = null;
    }
    assertEquals(tester, logined);
  }
}
