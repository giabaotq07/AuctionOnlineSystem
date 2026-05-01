package app.server.dao.impl;

import app.enums.UserRole;
import app.models.Admin;
import app.models.Bidder;
import app.models.Seller;
import app.models.User;
import app.server.config.connection;
import app.server.dao.UserDAO;
import app.server.exception.DaoException;
import java.sql.*;
import java.util.Optional;

public class MySqlUserDAO implements UserDAO {
  private final connection connection = connection.getInstance();

  @Override
  public int create(User user) {
    String sql = "INSERT INTO users(username, password_hash, role, balance) VALUES(?,?,?,?)";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, user.getUsername());
      stmt.setString(2, user.getPasswordHash());
      stmt.setString(3, user.getRole().name());
      stmt.setDouble(4, user.getBalance());
      stmt.executeUpdate();
      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
      return 0;
    } catch (SQLException e) {
      throw new DaoException("Failed to create user", e);
    }
  }

  @Override
  public Optional<User> findByUsername(String username) {
    String sql = "SELECT * FROM users WHERE username = ?";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, username);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapUser(rs));
        }
      }
      return Optional.empty();
    } catch (SQLException e) {
      throw new DaoException("Failed to find user by username", e);
    }
  }

  @Override
  public Optional<User> findById(int id) {
    String sql = "SELECT * FROM users WHERE id = ?";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapUser(rs));
        }
      }
      return Optional.empty();
    } catch (SQLException e) {
      throw new DaoException("Failed to find user by id", e);
    }
  }

  private User mapUser(ResultSet rs) throws SQLException {
    int id = rs.getInt("id");
    String username = rs.getString("username");
    String passwordHash = rs.getString("password_hash");
    UserRole role = UserRole.valueOf(rs.getString("role"));
    double balance = rs.getDouble("balance");

    return switch (role) {
      case ADMIN -> new Admin(id, username, passwordHash, balance);
      case SELLER -> new Seller(id, username, passwordHash, balance);
      case BIDDER -> new Bidder(id, username, passwordHash, balance);
    };
  }
}

