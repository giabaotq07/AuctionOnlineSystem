package app.dao.impl;

import app.dao.BaseDAO;
import app.dao.UserDAO;
import app.enums.UserRole;
import app.exception.DatabaseException;
import app.exception.ServiceException;
import app.models.Account;
import app.models.User;
import app.models.UserFactory;
import app.models.Wallet;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlUserDAO extends BaseDAO implements UserDAO {

  public MySqlUserDAO() {}

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

  @Override
  public Optional<User> findById(int id) {
    return withConnection(
        conn -> findOne(conn, BASE_SELECT + "WHERE id = ?", id), "Lỗi kết nối khi tải user.");
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return withConnection(
        conn -> findOne(conn, BASE_SELECT + "WHERE username = ?", username),
        "Lỗi kết nối khi tải user theo username.");
  }

  @Override
  public List<User> findAll() {
    return withConnection(conn -> findAll(conn), "Lỗi kết nối khi tải danh sách users.");
  }

  private List<User> findAll(Connection conn) {
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

  @Override
  public User save(User user) {
    return withConnection(conn -> save(conn, user), "Lỗi kết nối khi thêm user.");
  }

  private User save(Connection conn, User user) {
    String sql =
        """
        INSERT INTO users (username, password, full_name, assets, role)
        VALUES (?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, user.getAccount().getUsername());
      // Expect password to be already hashed by service layer
      ps.setString(2, user.getAccount().getPassword());
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
        return user;
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi thêm user.", e);
    }
  }

  @Override
  public void update(User user) {
    if (user.getId() <= 0) {
      throw new DatabaseException("Id user không hợp lệ để cập nhật.");
    }
    runWithConnection(
        conn -> {
          int rows =
              executeUpdate(
                  conn,
                  "UPDATE users SET username = ?, password = ?, full_name = ?, assets = ?, role = ? WHERE id = ?",
                  user.getAccount().getUsername(),
                  // Expect password to be already hashed by service layer
                  user.getAccount().getPassword(),
                  user.getName(),
                  user.getWallet().getAssets(),
                  user.getRole().name(),
                  user.getId());
          if (rows == 0) {
            throw new DatabaseException("Không tìm thấy user để cập nhật.");
          }
        },
        "Lỗi kết nối khi cập nhật user.");
  }

  @Override
  public void deleteAll() {
    runWithConnection(
        conn -> {
          try {
            conn.createStatement().execute("DELETE FROM users");
          } catch (SQLException e) {
            throw new DatabaseException("Lỗi kết nối", e);
          }
        },
        "Failed to clean users");
  }

  @Override
  public void adjustWallet(int id, long delta) {
    runInTransaction(
        conn -> {
          // Lock the row for update to prevent race conditions
          String lockSql = "SELECT assets FROM users WHERE id = ? FOR UPDATE";
          long currentAssets;
          try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
              if (!rs.next()) {
                throw new ServiceException("Người dùng không tồn tại: " + id);
              }
              currentAssets = rs.getLong("assets");
            }
          } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi khóa hàng người dùng.", e);
          }

          // Validate sufficient balance for withdrawals
          if (delta < 0 && currentAssets < -delta) {
            throw new ServiceException("Số dư không đủ để thực hiện giao dịch.");
          }

          // Update with new balance
          String updateSql = "UPDATE users SET assets = assets + ? WHERE id = ?";
          try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setLong(1, delta);
            ps.setInt(2, id);
            ps.executeUpdate();
          } catch (SQLException e) {
            throw new DatabaseException("Lỗi khi cập nhật ví.", e);
          }
        },
        "Lỗi kết nối khi cập nhật ví.");
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
