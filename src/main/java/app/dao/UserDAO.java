package app.dao;

import app.config.DatabaseConnection;
import app.config.PasswordUtils;
import app.exceptions.UserAlreadyExistsException;
import app.exceptions.UserNotFoundException;
import app.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

  // Helper method để tái sử dụng logic mapping từ ResultSet sang Object
  private User mapUser(ResultSet rs) throws SQLException {
    User user = new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("account"),
            rs.getString("password")
    );
    // Nếu model User có thêm các field này, hãy bổ sung:
    // user.setRole(rs.getString("role"));
    // user.setAssets(rs.getDouble("assets"));
    return user;
  }

  public boolean checkLogin(String account, String password) {
    String query = "SELECT id FROM users WHERE account = ? AND password = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setString(1, account);
      pstmt.setString(2, PasswordUtils.hashPassword(password));

      try (ResultSet rs = pstmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi hệ thống khi xác thực đăng nhập.", e);
    }
  }

  public User loadUsers(String account) throws UserNotFoundException {
    String query = "SELECT * FROM users WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setString(1, account);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return mapUser(rs);
        }
        throw new UserNotFoundException("Không tìm thấy người dùng: " + account);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database khi tải thông tin user.", e);
    }
  }

  public User addUser(User user) throws UserAlreadyExistsException {
    String insertSql = "INSERT INTO users (account, password, name) VALUES (?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

      pstmt.setString(1, user.getAccount());
      pstmt.setString(2, PasswordUtils.hashPassword(user.getPassword()));
      pstmt.setString(3, user.getName());

      pstmt.executeUpdate();
      try (ResultSet rs = pstmt.getGeneratedKeys()) {
        if (rs.next()) {
          user.setId(rs.getInt(1));
        }
        return user;
      }
    } catch (SQLException e) {
      if (e.getErrorCode() == 1062) {
        throw new UserAlreadyExistsException("Tài khoản '" + user.getAccount() + "' đã tồn tại!");
      }
      throw new RuntimeException("Lỗi database khi thêm user.", e);
    }
  }

  public void deleteUser(String account) throws UserNotFoundException {
    String deleteSql = "DELETE FROM users WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {

      pstmt.setString(1, account);
      int rows = pstmt.executeUpdate();
      if (rows == 0) {
        throw new UserNotFoundException("Không thể xóa. User '" + account + "' không tồn tại.");
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database khi xóa user.", e);
    }
  }

  public User updateUser(User user) throws UserNotFoundException {
    String updateSql = "UPDATE users SET name = ?, password = ? WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

      pstmt.setString(1, user.getName());
      pstmt.setString(2, PasswordUtils.hashPassword(user.getPassword()));
      pstmt.setString(3, user.getAccount());

      if (pstmt.executeUpdate() == 0) {
        throw new UserNotFoundException("Không thể cập nhật. User '" + user.getAccount() + "' không tồn tại.");
      }
      return user;
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database khi cập nhật user.", e);
    }
  }

  public void updateUserBalance(User user) throws UserNotFoundException {
    String updateSql = "UPDATE users SET assets = ? WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

      pstmt.setDouble(1, user.getAssets());
      pstmt.setString(2, user.getAccount());

      if (pstmt.executeUpdate() == 0) {
        throw new UserNotFoundException("Không tìm thấy user để cập nhật số dư.");
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database khi cập nhật số dư.", e);
    }
  }

  public List<User> getAllUsers() {
    String query = "SELECT * FROM users";
    List<User> list = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query);
         ResultSet rs = pstmt.executeQuery()) {

      while (rs.next()) {
        list.add(mapUser(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database khi lấy danh sách user.", e);
    }
    return list;
  }

  public User getUserById(int id) throws UserNotFoundException {
    String query = "SELECT * FROM users WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setInt(1, id);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return mapUser(rs);
        }
        throw new UserNotFoundException("Không tìm thấy user với ID: " + id);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database khi truy vấn ID.", e);
    }
  }

  public String getUserRole(String account) {
    String query = "SELECT role FROM users WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setString(1, account);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("role");
        }
        return null; // Role có thể null nếu không tìm thấy
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi database khi lấy quyền user.", e);
    }
  }
}