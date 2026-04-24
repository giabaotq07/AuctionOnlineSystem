package app.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.config.DatabaseConnection;
import app.dao.UserDAO;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserDaoTest {
  UserDAO userDAO;
  User user0;

  @BeforeEach
  void setUp() {
    try {
      DatabaseConnection.getConnection();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    userDAO = new UserDAO();
    userDAO.deleteUser("test_account");
    user0 = userDAO.addUser("test_account", "test_password", "Test User");
  }

  @Test
  void testLoadUser() {
    User user1 = userDAO.loadUsers("test_account");
    assertEquals(user1, user0);
  }
}
