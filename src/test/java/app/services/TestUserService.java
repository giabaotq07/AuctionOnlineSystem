package app.services;

import app.config.DatabaseConnection;
import app.dao.UserDAO;
import app.models.*;
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
        tester = UserFactory.createUser(
                "Test User", new Account("test_account", "test_password"), new Wallet(), UserRole.BIDDER.name());
        tester = userDAO.addUser(tester);
    }

    @Test
    public void testLogin() {
        User logined = userService.login("test_account", "test_password");
        assertEquals(tester, logined);
    }
}
