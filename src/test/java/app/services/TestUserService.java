package app.services;

import app.config.DatabaseConnection;
import app.dao.UserDAO;
import app.models.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUserService {
    static User tester;
    static UserDAO userDAO;
    static UserService userService;

    @BeforeAll
    public static void setupDatabase() {
        DatabaseConnection.initializeDatabase();
        userDAO = new UserDAO();
        userService = new UserService(userDAO);
        tester = new User(12, "Test User", "test_account", "test_password");
    }

    @Test
    public void testLogin() {
        User loged = userService.login("test_account", "test_password");
        assertEquals(tester, loged);
    }
}
