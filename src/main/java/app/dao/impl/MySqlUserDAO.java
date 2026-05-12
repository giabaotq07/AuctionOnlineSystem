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
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlUserDAO extends BaseDAO implements UserDAO {
  public MySqlUserDAO() {}

  private static final String TABLE = "users";
  private static final String BASE_SELECT =
      "SELECT id, username, password, full_name, available_balance, frozen_funds, role FROM users ";

  private User mapUser(ResultSet rs) throws SQLException {
    BigDecimal available = rs.getBigDecimal("available_balance");
    String frozenJson = rs.getString("frozen_funds");
    return UserFactory.createUser(
        rs.getInt("id"),
        rs.getString("full_name"),
        new Account(rs.getString("username"), rs.getString("password")),
        new Wallet(available, Wallet.parseFrozenFunds(frozenJson)),
        UserRole.valueOf(rs.getString("role")));
  }

  @Override
  public Optional<User> findById(int id) {
    return withConnection(
        conn -> findOne(conn, BASE_SELECT + "WHERE id = ?", id), "Lỗi kết nối khi tải user.");
  }

  @Override
  public Optional<User> findById(Connection conn, int id) {
    return findOne(conn, BASE_SELECT + "WHERE id = ?", id);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return withConnection(
        conn -> findOne(conn, BASE_SELECT + "WHERE username = ?", username),
        "Lỗi kết nối khi tải user theo username.");
  }

  @Override
  public Optional<User> findByUsername(Connection conn, String username) {
    return findOne(conn, BASE_SELECT + "WHERE username = ?", username);
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
    return withConnection(conn -> saveInternal(conn, user), "Lỗi kết nối khi thêm user.");
  }

  @Override
  public User save(Connection conn, User user) {
    return saveInternal(conn, user);
  }

  private User saveInternal(Connection conn, User user) {
    String sql =
        """
        INSERT INTO users (username, password, full_name, available_balance, frozen_funds, role)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, user.getAccount().getUsername());
      // Expect password to be already hashed by service layer
      ps.setString(2, user.getAccount().getPassword());
      ps.setString(3, user.getName());
      ps.setBigDecimal(4, user.getWallet().getAvailableBalance());
      ps.setString(5, user.getWallet().serializeFrozenFunds());
      ps.setString(6, user.getRole().name());
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
    runWithConnection(conn -> update(conn, user), "Lỗi kết nối khi cập nhật user.");
  }

  @Override
  public void update(Connection conn, User user) {
    boolean ok =
        executeUpdate(
            conn,
            TABLE,
            "UPDATE users SET username = ?, password = ?, full_name = ?, available_balance = ?, frozen_funds = ?, role = ? WHERE id = ?",
            user.getAccount().getUsername(),
            // Expect password to be already hashed by service layer
            user.getAccount().getPassword(),
            user.getName(),
            user.getWallet().getAvailableBalance(),
            user.getWallet().serializeFrozenFunds(),
            user.getRole().name(),
            user.getId());
    if (!ok) {
      throw new DatabaseException("Không tìm thấy user để cập nhật.");
    }
  }

  @Override
  public void lockRow(Connection conn, int id) {
    String sql = "SELECT id FROM users WHERE id = ? FOR UPDATE";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          throw new ServiceException("Người dùng không tồn tại: " + id);
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi khóa hàng người dùng.", e);
    }
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
}
