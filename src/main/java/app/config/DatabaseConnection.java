package app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConnection {

  private static final HikariDataSource dataSource;

  static {
    HikariConfig config = new HikariConfig();

    config.setJdbcUrl(DatabaseConfig.load().getUrl());
    config.setUsername(DatabaseConfig.load().getUser());
    config.setPassword(DatabaseConfig.load().getPassword());

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
}
