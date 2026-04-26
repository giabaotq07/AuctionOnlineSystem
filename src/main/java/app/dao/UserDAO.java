package app.dao;

import app.config.DatabaseConnection;
import app.config.PasswordUtils;
import app.exceptions.DatabaseException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO implements GenericDAO<User, Integer> {
  private User mapUser(ResultSet rs) throws SQLException {
    return UserFactory.createUser(
        rs.getInt("id"),
        rs.getString("name"),
        new Account(rs.getString("account"), rs.getString("password")),
        new Wallet(rs.getDouble("assets")),
        rs.getString("role"));
  }

  public User getUserByAccount(String account) {
    String query = "SELECT * FROM users WHERE account = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setString(1, account);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) return mapUser(rs);
      }
      return null;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi lấy user theo account.", e);
    }
  }

  @Override
  public User getById(Integer id) {
    String query = "SELECT * FROM users WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {

      pstmt.setInt(1, id);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) return mapUser(rs);
      }
      return null;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi truy vấn user theo ID.", e);
    }
  }

  @Override
  public List<User> getAll() {
    String query = "SELECT * FROM users";
    List<User> list = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query);
        ResultSet rs = pstmt.executeQuery()) {

      while (rs.next()) {
        list.add(mapUser(rs));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi lấy danh sách user.", e);
    }
    return list;
  }

  @Override
  public User add(User user) {
    String insertSql =
        "INSERT INTO users (account, password, name, role, assets) VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt =
            conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setString(1, user.getAccount().getUsername());
      pstmt.setString(2, PasswordUtils.hashPassword(user.getAccount().getPassword()));
      pstmt.setString(3, user.getName());
      pstmt.setString(4, user.getRole().name());
      pstmt.setDouble(5, user.getWallet().getAssets());
      pstmt.executeUpdate();
      try (ResultSet rs = pstmt.getGeneratedKeys()) {
        if (rs.next()) {
          user.setId(rs.getInt(1));
          return user;
        }
        return null;
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi database khi thêm user (có thể trùng account).", e);
    }
  }

  @Override
  public boolean update(User user) {
    // Basic update updating all fields
    String sql = "UPDATE users SET name = ?, assets = ?, role = ? WHERE id = ?";
    return executeUpdate(sql, user.getName(), user.getWallet().getAssets(), user.getRole().name(), user.getId());
  }

  private boolean executeUpdate(String sql, Object... params) {
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        pstmt.setObject(i + 1, params[i]);
      }
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi thực thi lệnh Update SQL", e);
    }
  }

  public boolean updateUserProfile(User user) {
    String sql = "UPDATE users SET name = ? WHERE id = ?";
    return executeUpdate(sql, user.getName(), user.getId());
  }

  public boolean updateUserWallet(User user) {
    String sql = "UPDATE users SET assets = ? WHERE id = ?";
    return executeUpdate(sql, user.getWallet().getAssets(), user.getId());
  }

  public boolean updateUserPassword(User user, String newPassword) {
    String sql = "UPDATE users SET password = ? WHERE id = ?";
    return executeUpdate(sql, PasswordUtils.hashPassword(newPassword), user.getId());
  }

  @Override
  public boolean delete(Integer id) {
    String sql = "DELETE FROM users WHERE id = ?";
    return executeUpdate(sql, id);
  }
}
