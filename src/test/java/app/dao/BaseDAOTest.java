package app.dao;

import app.config.DatabaseConnection;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseDAOTest {

  @BeforeAll
  void setupDatabase() throws Exception {
    System.setProperty("db.url", "jdbc:mysql://localhost:3306/auction_db_test");
    System.setProperty("db.user", "root");
    System.setProperty("db.password", "123456");

    Field field = DatabaseConnection.class.getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null);

    String sql =
        new String(
            Objects.requireNonNull(BaseDAOTest.class.getResourceAsStream("/schema.sql"))
                .readAllBytes());

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
        Statement stmt = conn.createStatement()) {
      for (String statement : sql.split(";")) {
        String trimmed = statement.trim();
        if (!trimmed.isEmpty()) {
          stmt.execute(trimmed);
        }
      }
    }
  }
}
