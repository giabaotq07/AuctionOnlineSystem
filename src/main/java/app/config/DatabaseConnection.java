package app.config;

import app.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
  private static volatile DatabaseConnection instance;
  private final DatabaseConfig config;

  private DatabaseConnection(DatabaseConfig config) {
    this.config = config;
  }

  public static DatabaseConnection getInstance() {
    if (instance == null) {
      synchronized (DatabaseConnection.class) {
        if (instance == null) {
            instance = new DatabaseConnection(DatabaseConfig.load());
        }
      }
    }
    return instance;
  }

  public Connection getConnection() {
    try {
      return DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
    } catch (SQLException e) {
      throw new DatabaseException("Unable to connect to database", e);
    }
  }
}
