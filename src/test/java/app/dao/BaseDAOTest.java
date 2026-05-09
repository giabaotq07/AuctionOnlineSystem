package app.dao;

import app.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseDAOTest {

  private static final Logger logger = LoggerFactory.getLogger(BaseDAOTest.class);

  private static final String TEST_DB_URL = "jdbc:mysql://localhost:3306/";
  private static final String TEST_DB_NAME = "auction_db_test";
  private static final String TEST_DB_FULL_URL = TEST_DB_URL + TEST_DB_NAME;

  private static final String USER = "root";
  private static final String PASS = "123456";

  @BeforeAll
  void setupDatabase() throws Exception {

    logger.info("Setting up test database...");

    // 1. Create DB if not exists
    try (Connection conn = DriverManager.getConnection(TEST_DB_URL, USER, PASS);
        Statement stmt = conn.createStatement()) {

      stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + TEST_DB_NAME);
      logger.info("Database {} ensured.", TEST_DB_NAME);

    } catch (Exception e) {
      logger.warn("Could not create database automatically", e);
    }

    // 2. Inject test config (IMPORTANT: BEFORE HIKARI INIT)
    System.setProperty("db.url", TEST_DB_FULL_URL);
    System.setProperty("db.user", USER);
    System.setProperty("db.password", PASS);

    // 3. Reload connection pool (Hikari-safe reset)
    DatabaseConnection.reload(); // 👈 thêm hàm này (quan trọng)

    // 4. Load schema
    logger.info("Initializing schema...");

    String sql =
        new String(
            Objects.requireNonNull(BaseDAOTest.class.getResourceAsStream("/schema.sql"))
                .readAllBytes());

    try (Connection conn = DatabaseConnection.getConnection();
        Statement stmt = conn.createStatement()) {

      for (String s : sql.split(";")) {
        String trimmed = s.trim();
        if (!trimmed.isEmpty()) {
          stmt.execute(trimmed);
        }
      }

      logger.info("Schema initialized successfully.");

    } catch (Exception e) {
      logger.error("Schema initialization failed", e);
      throw e;
    }
  }
}
