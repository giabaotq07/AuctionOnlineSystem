package app.dao;

import app.config.DatabaseConnection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseDAOTest {

  private static final Logger logger = LoggerFactory.getLogger(BaseDAOTest.class);

  private static final String DB_HOST = "jdbc:mysql://localhost:3306/";
  private static final String TEST_DB = "auction_db_test";
  private static final String FULL_URL = DB_HOST + TEST_DB;

  private static final String USER = System.getProperty("db.user", "root");
  private static final String PASS = System.getProperty("db.password", "123456");

  // =========================
  // GLOBAL SETUP (RUN ONCE)
  // =========================
  @BeforeAll
  void globalSetup() {
    createDatabaseIfNotExists();
    configureTestEnvironment();
    reloadConnectionPool(); // FIXED
    initSchema();
  }

  // =========================
  // CLEAN BEFORE EACH TEST
  // =========================
  @BeforeEach
  void cleanData() {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection();
        Statement stmt = conn.createStatement()) {

      stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

      // safer than truncate in FK-heavy schema
      stmt.execute("DELETE FROM bids");
      stmt.execute("DELETE FROM items");

      stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

    } catch (Exception e) {
      throw new RuntimeException("Failed to clean test data", e);
    }
  }

  // =========================
  // DB CREATE
  // =========================
  private void createDatabaseIfNotExists() {
    logger.info("[DB] Ensuring test database exists...");

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(DB_HOST);
    config.setUsername(USER);
    config.setPassword(PASS);

    try (HikariDataSource dataSource = new HikariDataSource(config);
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {

      stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + TEST_DB);
      logger.info("[DB] Ready: {}", TEST_DB);

    } catch (Exception e) {
      throw new RuntimeException("Failed to create test database", e);
    }
  }

  // =========================
  // CONFIG TEST ENV
  // =========================
  private void configureTestEnvironment() {
    logger.info("[CONFIG] Setting test DB environment...");

    System.setProperty("db.url", FULL_URL);
    System.setProperty("db.user", USER);
    System.setProperty("db.password", PASS);
  }

  // =========================
  // FIXED POOL HANDLING
  // =========================
  private void reloadConnectionPool() {
    logger.info("[POOL] Reloading Hikari pool safely...");
    // Lúc này System.setProperty đã được set bởi configureTestEnvironment()
    // nên init() trong resetDataSource sẽ đọc đúng URL test DB
    DatabaseConnection.resetDataSource();
  }

  // =========================
  // INIT SCHEMA
  // =========================
  private void initSchema() {
    logger.info("[SCHEMA] Initializing schema...");

    String sql = loadSchemaFile();

    try (Connection conn = DatabaseConnection.getDataSource().getConnection();
        Statement stmt = conn.createStatement()) {

      executeSqlScript(stmt, sql);
      logger.info("[SCHEMA] OK");

    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize schema", e);
    }
  }

  // =========================
  // LOAD SCHEMA
  // =========================
  private String loadSchemaFile() {
    try {
      return new String(
          Objects.requireNonNull(BaseDAOTest.class.getResourceAsStream("/schema.sql"))
              .readAllBytes());
    } catch (Exception e) {
      throw new RuntimeException("Cannot load schema.sql", e);
    }
  }

  // =========================
  // EXEC SQL SCRIPT
  // =========================
  private void executeSqlScript(Statement stmt, String sql) throws Exception {
    for (String statement : sql.split(";")) {
      String trimmed = statement.trim();
      if (!trimmed.isEmpty()) {
        stmt.execute(trimmed);
      }
    }
  }
}
