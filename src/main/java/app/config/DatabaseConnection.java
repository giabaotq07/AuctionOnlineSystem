package app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConnection {

  private static HikariDataSource dataSource;

  static {
    init();
  }

  private static void init() {
    HikariConfig config = new HikariConfig();

    DatabaseConfig cfg = DatabaseConfig.load();

    config.setJdbcUrl(cfg.getUrl());
    config.setUsername(cfg.getUser());
    config.setPassword(cfg.getPassword());

    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setIdleTimeout(600000);
    config.setConnectionTimeout(30000);
    config.setMaxLifetime(1800000);

    dataSource = new HikariDataSource(config);
  }

  private DatabaseConnection() {}

  public static Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public static synchronized void reload() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
    init(); // 🔥 quan trọng: rebuild pool
  }
}
