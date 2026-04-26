package app.dao;

import app.config.DatabaseConnection;
import app.config.PasswordUtils;
import app.exceptions.DatabaseException;
import app.models.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
  private User mapUser(ResultSet rs) throws SQLException {
    return new User(
        rs.getInt("id"), rs.getString("name"), rs.getString("account"), rs.getString("password"));
  }

  public User loadUsers(String account) {
    String query = "SELECT * FROM users WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setString(1, account);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return mapUser(rs);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi lấy user theo account.", e);
    }
  }

  public User addUser(User user) {
    String insertSql = "INSERT INTO users (account, password, name) VALUES (?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt =
            conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
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
      throw new DatabaseException("Lỗi database khi thêm user (có thể trùng account).", e);
    }
  }

  public boolean deleteUser(String account) {
    String deleteSql = "DELETE FROM users WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
      pstmt.setString(1, account);
      int rows = pstmt.executeUpdate();
      return rows > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi xóa user.", e);
    }
  }

  public boolean updateUser(User user) {
    String updateSql = "UPDATE users SET name = ?, password = ? WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
      pstmt.setString(1, user.getName());
      pstmt.setString(2, PasswordUtils.hashPassword(user.getPassword()));
      pstmt.setString(3, user.getAccount());
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi cập nhật user.", e);
    }
  }

  public boolean updateUserBalance(User user) {
    String updateSql = "UPDATE users SET assets = ? WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
      pstmt.setDouble(1, user.getAssets());
      pstmt.setString(2, user.getAccount());
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi cập nhật số dư user.", e);
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
      throw new DatabaseException("Lỗi database khi lấy danh sách user.", e);
    }
    return list;
  }

  public User getUserById(int id) {
    String query = "SELECT * FROM users WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, id);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return mapUser(rs);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi truy vấn user theo ID.", e);
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
        return null;
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi lấy quyền user.", e);
    }
  }
}
