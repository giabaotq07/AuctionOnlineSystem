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
  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(URL, USER, PASSWORD);
  }

  public static void closeConnection() {
    if (connection != null) {
      try {
        connection.close();
        System.out.println("Đóng kết nối MySQL thành công!");
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
