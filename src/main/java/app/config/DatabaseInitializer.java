package app.config;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {
  public static void initDatabase() {
    try {
      Connection conn = DatabaseConnection.getConnection();
      Statement stmt = conn.createStatement();

      // 1. Tạo database nếu chưa tồn tại
      stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS auction_db");
      stmt.executeUpdate("USE auction_db");

      // 2. Tạo các bảng cần thiết
      stmt.executeUpdate(
          "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password VARCHAR(50) NOT NULL)");

      System.out.println("Database auction_db đã sẵn sàng!");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
