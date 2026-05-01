package app.config;

import app.exception.DaoException;
import app.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
  private static volatile DatabaseConnection instance;
  private final Connection connection;

  private DatabaseConnection() throws SQLException {
    DatabaseConfig config = DatabaseConfig.load();
    connection =
        DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
  }

  public static DatabaseConnection getInstance() {
    if (instance == null) {
      synchronized (DatabaseConnection.class) {
        if (instance == null) {
          try {
            instance = new DatabaseConnection();
          } catch (SQLException e) {
            throw new DatabaseException("Không thể khởi tạo kết nối.", e);
          }
        }
      }
    }
    return instance;
  }

  public Connection getConnection() {
    return connection;
  }
}
