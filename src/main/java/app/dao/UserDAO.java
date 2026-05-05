package app.dao;

import app.enums.UserRole;
import app.exception.DatabaseException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import app.utils.PasswordUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

  public UserDAO() {}

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

  public Optional<User> findById(Connection conn, int id) {
    return findOne(conn, BASE_SELECT + "WHERE id = ?", id);
  }

  public Optional<User> findByUsername(Connection conn, String username) {
    return findOne(conn, BASE_SELECT + "WHERE username = ?", username);
  }

  public List<User> findAll(Connection conn) {
    String sql = BASE_SELECT + "ORDER BY id";
    List<User> users = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        users.add(mapUser(rs));
      }
      return users;
    } catch (SQLException e) {
      throw new DatabaseException("Không thể lấy danh sách users.", e);
    }
  }

  public User save(Connection conn, User user) {
    String sql =
        """
        INSERT INTO users (username, password, full_name, assets, role)
        VALUES (?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
      throw new DatabaseException("Lỗi khi thêm user.", e);
    }
  }

  public void updateProfile(Connection conn, int id, String fullName) {
    executeUpdate(conn, "UPDATE users SET full_name = ? WHERE id = ?", fullName, id);
  }

  public void updateUsername(Connection conn, int id, String newUsername) {
    executeUpdate(conn, "UPDATE users SET username = ? WHERE id = ?", newUsername, id);
  }

  public void updatePassword(Connection conn, int id, String newPassword) {
    executeUpdate(
        conn,
        "UPDATE users SET password = ? WHERE id = ?",
        PasswordUtils.hashPassword(newPassword),
        id);
  }

  public void adjustWallet(Connection conn, int id, long delta) {
    int rows;
    if (delta < 0) {
      rows =
          executeUpdate(
              conn,
              "UPDATE users SET assets = assets + ? WHERE id = ? AND assets >= ?",
              delta,
              id,
              -delta);
      if (rows == 0) {
        throw new DatabaseException("Số dư không đủ để thực hiện giao dịch.");
      }
    } else {
      executeUpdate(conn, "UPDATE users SET assets = assets + ? WHERE id = ?", delta, id);
    }
  }

  private Optional<User> findOne(Connection conn, String sql, Object... params) {
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private int executeUpdate(Connection conn, String sql, Object... params) {
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      setParameters(ps, params);
      return ps.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi truy vấn bảng " + TABLE, e);
    }
  }

  private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      ps.setObject(i + 1, params[i]);
    }
  }
}
