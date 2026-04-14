package Server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseInitializer {
  public static void initDatabase() {
    String url = AppConfig.get("db.url");
    String dbName = AppConfig.get("db.name");

    try (Connection conn =
            DriverManager.getConnection(
                url, AppConfig.get("db.user"), AppConfig.get("db.password"));
        Statement stmt = conn.createStatement()) {

      // 1. Tạo database nếu chưa tồn tại
      stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
      stmt.executeUpdate("USE " + dbName);

      // 2. Tạo các bảng cần thiết
      stmt.executeUpdate(
          "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password VARCHAR(50) NOT NULL)");

      System.out.println("Database '" + dbName + "' đã sẵn sàng!");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
