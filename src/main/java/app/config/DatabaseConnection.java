package app.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
  private static Connection connection = null;
  private static final String URL = "jdbc:mysql://localhost:3306/auction_db";
  private static final String USER = "root"; // Đổi thành user MySQL của cậu
  private static final String PASSWORD = "25122007"; // Đổi thành pass MySQL của cậu

  // Private constructor để ngăn tạo object từ bên ngoài
  private DatabaseConnection() {}

  public static Connection getConnection() {
    if (connection == null) {
      try {
        // Đăng ký Driver (tùy chọn với bản MySQL mới nhưng nên có để chắc chắn)
        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
        System.out.println("Kết nối MySQL thành công!");
      } catch (ClassNotFoundException | SQLException e) {
        e.printStackTrace();
      }
    }
    return connection;
  }
}
