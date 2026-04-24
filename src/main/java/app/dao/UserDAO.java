package app.dao;

import app.config.DatabaseConnection;
import app.config.PasswordUtils;
import app.models.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
  // Trả về true nếu đăng nhập thành công
  public boolean checkLogin(String account, String password) {
    String query = "SELECT id FROM users WHERE account = ? AND password = ?";

    // Dùng try-with-resources để tự động đóng kết nối (rất hợp với JDK bản mới)
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setString(1, account);
      pstmt.setString(
          2,
          PasswordUtils.hashPassword(
              password)); // Ở hệ thống thực tế, bạn sẽ phải hash mật khẩu trước khi so sánh

      try (ResultSet rs = pstmt.executeQuery()) {
        return rs.next(); // Nếu ResultSet có dữ liệu -> Sai/Đúng
      }

    } catch (SQLException e) {
      return false;
    }
  }

  public User loadUsers(String account) {
    String query = "SELECT * FROM users WHERE account = ?";
    User user = null;
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, account);
      ResultSet rs = pstmt.executeQuery();

      while (rs.next()) {
        String id = rs.getString("id");
        String username = rs.getString("account");
        String password = rs.getString("password");
        String name = rs.getString("name");
        user = new User(id, name, username, password);
        return user;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return user;
  }

  public User addUser(String account, String password, String name) {
    String insertSql = "INSERT INTO users (account, password, name) VALUES (?, ?, ?)";
    int generatedId = -1;

    // 1. Chú ý tham số thứ 2: Statement.RETURN_GENERATED_KEYS
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

      pstmt.setString(1, account);
      pstmt.setString(2, PasswordUtils.hashPassword(password)); // Nhớ hash mật khẩu nhé!
      pstmt.setString(3, name);

      // 2. Chạy lệnh INSERT
      int rowsAffected = pstmt.executeUpdate();

      // 3. Nếu INSERT thành công (ảnh hưởng > 0 dòng)
      if (rowsAffected > 0) {
        // Lấy ra danh sách các khóa (ID) vừa được tạo
        try (ResultSet rs = pstmt.getGeneratedKeys()) {
          if (rs.next()) {
            generatedId = rs.getInt(1); // Cột 1 chính là ID tự tăng
            System.out.println("Đăng ký thành công! ID tự động của user là: " + generatedId);
            return loadUsers(account); // Trả về user vừa tạo dựa trên account
          }
        }
      }
    } catch (SQLException e) {
      // Mã 1062 là Duplicate Entry (Trùng lặp khóa chính hoặc cột UNIQUE)
      if (e.getErrorCode() == 1062) {
        System.out.println("Tài khoản '" + account + "' đã có người sử dụng!");
      } else {
        System.err.println("Lỗi SQL khi thêm User: " + e.getMessage());
        e.printStackTrace(); // In ra lỗi khác nếu có
      }
    } catch (Exception ex) {
      System.err.println("Lỗi không xác định khi thêm User: " + ex.getMessage());
      ex.printStackTrace();
    }
    return null; // Trả về null nếu có lỗi hoặc không thành công
  }

  public boolean deleteUser(String account) {
    String deleteSql = "DELETE FROM users WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
      pstmt.setString(1, account);
      int rowsAffected = pstmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("User deleted: " + account);
        return true; // Trả về true sau khi xóa thành công
      } else {
        System.out.println("User '" + account + "' không tồn tại.");
        return false; // Trả về false nếu không tìm thấy user
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public User updateUser(User user) {
    String updateSql = "UPDATE users SET name = ?, password = ? WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

      pstmt.setString(1, user.getName());
      pstmt.setString(2, PasswordUtils.hashPassword(user.getPassword()));
      pstmt.setString(3, user.getAccount());

      int rowsAffected = pstmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("User updated: " + user.getAccount());
        return loadUsers(user.getAccount());
      } else {
        System.out.println("User '" + user.getAccount() + "' không tồn tại.");
        return null;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void getUserRole(String account) {
    String query = "SELECT role FROM users WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, account); // Thay bằng account bạn muốn kiểm tra
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        String role = rs.getString("role");
        System.out.println("Role của user '" + account + "' là: " + role);
      } else {
        System.out.println("User '" + account + "' không tồn tại.");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void updateUserBalance(User user) {
    String updateSql = "UPDATE users SET assets = ? WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

      // Giả sử bạn có một User object với số dư mới
      // User user = new User("1", "John Doe", "john_doe", "password123");
      // user.Deposit(1000); // Cập nhật số dư mới

      pstmt.setDouble(1, user.getAssets());
      pstmt.setString(2, user.getAccount());

      int rowsAffected = pstmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("User balance updated: " + user.getAccount());
      } else {
        System.out.println("User '" + user.getAccount() + "' không tồn tại.");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void getAllUsers() {
    String query = "SELECT * FROM users";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query);
        ResultSet rs = pstmt.executeQuery()) {

      while (rs.next()) {
        String account = rs.getString("account");
        String name = rs.getString("name");
        System.out.println("Account: " + account + ", Name: " + name);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void getUserById(int id) {
    String query = "SELECT * FROM users WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        String account = rs.getString("account");
        String name = rs.getString("name");
        System.out.println("User ID: " + id + ", Account: " + account + ", Name: " + name);
      } else {
        System.out.println("User with ID '" + id + "' không tồn tại.");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
