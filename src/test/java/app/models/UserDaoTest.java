package app.models;

import app.config.DatabaseConnection;
import app.dao.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
