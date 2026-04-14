package Server.dao;

import app.config.DatabaseConnection;
import app.config.DatabaseInitializer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
  public void loadUsers() {
    String query = "SELECT * FROM users";
    try {
      Connection conn = DatabaseConnection.getConnection();
      PreparedStatement pstmt = conn.prepareStatement(query);
      ResultSet rs = pstmt.executeQuery();

      while (rs.next()) {
        System.out.println("User: " + rs.getString("username"));
        System.out.println("Password: " + rs.getString("password"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void addUser(String username, String password) {
    String insertSql = "INSERT INTO users (username, password) VALUES (?, ?)";
    try {
      Connection conn = DatabaseConnection.getConnection();
      PreparedStatement pstmt = conn.prepareStatement(insertSql);
      pstmt.setString(1, username);
      pstmt.setString(2, password);
      pstmt.executeUpdate();
      System.out.println("User added: " + username);
    } catch (SQLException e) {
      if (e.getErrorCode() == 1062) { // get error code thử sẽ thấy code 1062 là code trùng key
        System.out.println("Username '" + username + "' đã tồn tại. Vui lòng chọn tên khác.");
      } else {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    DatabaseInitializer.initDatabase(); // Khởi tạo database và bảng nếu chưa tồn tại
    DatabaseConnection.getConnection(); // Đảm bảo kết nối được thiết lập
    UserDAO userDAO = new UserDAO();
    userDAO.addUser("testuser", "testpassword");
    userDAO.loadUsers();
    DatabaseConnection.closeConnection(); // Đóng kết nối sau khi sử dụng
  }
}
