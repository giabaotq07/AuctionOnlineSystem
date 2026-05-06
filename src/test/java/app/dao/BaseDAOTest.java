package app.dao;

import app.config.DatabaseConnection;
import java.lang.reflect.Field;
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

  @BeforeAll
  void setupDatabase() throws Exception {
    logger.info("Setting up database properties for tests...");

    // Create database if it doesn't exist
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "25122007");
         Statement stmt = conn.createStatement()) {
        stmt.execute("CREATE DATABASE IF NOT EXISTS auction_db_test");
        logger.info("Database auction_db_test ensured.");
    } catch (Exception e) {
        logger.warn("Could not create database automatically. Proceeding anyway...", e);
    }

    System.setProperty("db.url", "jdbc:mysql://localhost:3306/auction_db_test");
    System.setProperty("db.user", "root");
    System.setProperty("db.password", "25122007");

    try {
      Field field = DatabaseConnection.class.getDeclaredField("instance");
      field.setAccessible(true);
      field.set(null, null);
    } catch (Exception e) {
      logger.error("Failed to reset DatabaseConnection instance", e);
      throw e;
    }

    logger.info("Executing schema.sql to initialize test database...");
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
      logger.info("Database schema initialized successfully.");
    } catch (Exception e) {
      logger.error("Database initialization failed", e);
      throw e;
    }
  }
}
