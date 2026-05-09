package app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConnection {

  private static volatile HikariDataSource dataSource;

  static {
    init();
  }

  private DatabaseConnection() {}

  // =========================
  // INIT / REINIT CORE
  // =========================
  private static synchronized void init() {
    DatabaseConfig cfg = DatabaseConfig.load();

    HikariConfig config = new HikariConfig();
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

  private static synchronized void rebuild() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
    init();
  }

  // =========================
  // PUBLIC API
  // =========================
  public static Connection getConnection() throws SQLException {
    // defensive: nếu pool bị đóng thì tự rebuild
    if (dataSource == null || dataSource.isClosed()) {
      rebuild();
    }
    return dataSource.getConnection();
  }

  public static HikariDataSource getDataSource() {
    return dataSource;
  }

  public static synchronized void resetDataSource() {
    rebuild();
  }
}