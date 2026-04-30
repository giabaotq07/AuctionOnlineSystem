package app.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
  private static final String DB_HOST = valueOf("AUCTION_DB_HOST", "db.host", "localhost");
  private static final String DB_PORT = valueOf("AUCTION_DB_PORT", "db.port", "3306");
  private static final String DB_NAME = valueOf("AUCTION_DB_NAME", "db.name", "auction_db");
  private static final String USER = valueOf("AUCTION_DB_USER", "db.user", "root");
  private static final String PASSWORD =
      valueOf("AUCTION_DB_PASSWORD", "db.password", "123456"); // đổi pass
  private static final String SERVER_URL =
      "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/?allowMultiQueries=true&serverTimezone=UTC";

  private DatabaseConnection() {}

  private static String valueOf(String envKey, String propertyKey, String defaultValue) {
    String fromProperty = System.getProperty(propertyKey);
    if (fromProperty != null && !fromProperty.isBlank()) {
      return fromProperty;
    }
    String fromEnv = System.getenv(envKey);
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv;
    }
    return defaultValue;
  }

  public static Connection getConnection() throws SQLException {
    Connection conn = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
    try (Statement statement = conn.createStatement()) {
      statement.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
    }
    conn.setCatalog(DB_NAME);
    return conn;
  }

  public static void initializeDatabase() {
    try (Connection conn = getConnection();
        InputStream stream = DatabaseConnection.class.getResourceAsStream("/schema.sql")) {
      if (stream == null) {
        throw new IOException("schema.sql not found on classpath");
      }

      String schema = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      String[] statements = schema.split(";");
      for (String stmt : statements) {
        String sql = stmt.trim();
        if (sql.isEmpty()) {
          continue;
        }

        // Connection already points to DB_NAME, skip USE statements from schema files.
        if (sql.toUpperCase().startsWith("USE ")) {
          continue;
        }

        try (Statement statement = conn.createStatement()) {
          statement.execute(sql);
        }
      }

      System.out.println("Database initialization completed for '" + DB_NAME + "'.");
    } catch (Exception e) {
      System.out.println("Database initialization failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void closeConnection() {
    // Kept for compatibility with existing code.
  }
}
