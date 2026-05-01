package app.dao;

import app.config.DatabaseConnection;
import app.enums.UserRole;
import app.exception.DatabaseException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import app.util.PasswordUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
  private final DatabaseConnection databaseConnection = DatabaseConnection.getInstance();

  private static final String TABLE = "users";

  private static final String BASE_SELECT =
          "SELECT id, username, password, full_name, assets, role FROM users ";


  private User mapUser(ResultSet rs) throws SQLException {
    return UserFactory.createUser(
            rs.getInt("id"),
            rs.getString("full_name"),
            new Account(rs.getString("username"), rs.getString("password")),
            new Wallet(rs.getLong("assets")),
            UserRole.valueOf(rs.getString("role")));
  }

  public Optional<User> findById(int id) {
    return findOne(BASE_SELECT + "WHERE id = ?", id);
  }

  public Optional<User> findByUsername(String username) {
    return findOne(BASE_SELECT + "WHERE username = ?", username);
  }

  // Overload dùng trong transaction (Service truyền Connection vào)
  public Optional<User> findByUsername(Connection conn, String username) throws SQLException {
    String sql = BASE_SELECT + "WHERE username = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
      }
    }
  }

  public List<User> findAll() {
    String sql = BASE_SELECT + "ORDER BY id";
    List<User> users = new ArrayList<>();
    try (Connection conn = databaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        users.add(mapUser(rs));
      }
      return users;
    } catch (SQLException e) {
      throw new DatabaseException("Không thể lấy danh sách users.", e);
    }
  }

  public User save(User user) {
    String sql = """
        INSERT INTO users (username, password, full_name, assets, role)
        VALUES (?, ?, ?, ?, ?)
        """;
    try (Connection conn = databaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, user.getAccount().getUsername());
      ps.setString(2, PasswordUtils.hashPassword(user.getAccount().getPassword()));
      ps.setString(3, user.getName());
      ps.setLong(4, user.getWallet().getAssets());
      ps.setString(5, user.getRole().name());
      if (ps.executeUpdate() == 0) {
        throw new DatabaseException("Không thể thêm user.");
      }
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          user.setId(rs.getInt(1));
        }
      }
      return user;
    } catch (SQLException e) {
      if (e.getErrorCode() == 1062) {
        throw new DatabaseException("Username đã tồn tại.", e);
      }
      throw new DatabaseException("Lỗi khi thêm user.", e);
    }
  }

  public boolean updateProfile(int id, String fullName) {
    return executeUpdate("UPDATE users SET full_name = ? WHERE id = ?", fullName, id);
  }

  public boolean updatePassword(int id, String newPassword) {
    return executeUpdate(
            "UPDATE users SET password = ? WHERE id = ?",
            PasswordUtils.hashPassword(newPassword), id);
  }

  // delta dương = nạp tiền, delta âm = trừ tiền — atomic, tránh race condition
  public boolean adjustWallet(int id, long delta) {
    return executeUpdate("UPDATE users SET assets = assets + ? WHERE id = ?", delta, id);
  }

  public boolean delete(int id) {
    return executeUpdate("DELETE FROM users WHERE id = ?", id);
  }

  private Optional<User> findOne(String sql, Object... params) {
    try (Connection conn = databaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private boolean executeUpdate(String sql, Object... params) {
    try (Connection conn = databaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi cập nhật bảng " + TABLE, e);
    }
  }

  private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      ps.setObject(i + 1, params[i]);
    }
  }
}