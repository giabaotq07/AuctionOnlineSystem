package app.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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

  // giữ lại method này cho test
  public static synchronized void resetDataSource() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
    init(); // tạo pool mới với config hiện tại
  }

  // =========================
  // PUBLIC API
  // =========================
  public static HikariDataSource getDataSource() {
    return dataSource;
  }
}
