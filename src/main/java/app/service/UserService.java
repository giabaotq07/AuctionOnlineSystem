package app.service;

import app.database.DatabaseConnection;
import app.dao.UserDAO;
import app.exception.DatabaseException;
import app.exception.ServiceException;
import app.models.User;
import app.utils.PasswordUtils;
import java.sql.Connection;
import java.util.List;

public class UserService {
  private final UserDAO userDAO;

  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public User login(String username, String rawPassword) {
    User user = userDAO.findByUsername(username).orElse(null);
    if (user == null || !PasswordUtils.verify(rawPassword, user.getAccount().getPassword())) {
      throw new ServiceException("Tên đăng nhập hoặc mật khẩu không đúng");
    }
    return user;
  }

  public User register(User user) {
    validateNotBlank(user.getAccount().getUsername(), "Tên đăng nhập");
    validateNotBlank(user.getAccount().getPassword(), "Mật khẩu");
    validateNotBlank(user.getName(), "Họ tên");
    return runInTransaction(
        conn -> {
          if (userDAO.findByUsername(conn, user.getAccount().getUsername()).isPresent()) {
            throw new ServiceException("User đã tồn tại: " + user.getAccount().getUsername());
          }
          // Hash password in service layer before persisting (single responsibility)
          String hashed = PasswordUtils.hashPassword(user.getAccount().getPassword());
          user.getAccount().setPassword(hashed);
          return userDAO.save(conn, user);
        });
  }

  public void updateProfile(User user) {
    validateNotBlank(user.getName(), "Họ tên");
    runInTransaction(
        conn -> {
          User stored =
              userDAO
                  .findById(conn, user.getId())
                  .orElseThrow(
                      () -> new ServiceException("Không tìm thấy user với id: " + user.getId()));
          stored.setName(user.getName());
          userDAO.update(conn, stored);
        });
  }

  public void changePassword(String username, String oldPassword, String newPassword) {
    validateNotBlank(newPassword, "Mật khẩu mới");
    User user = login(username, oldPassword);
    String hashed = PasswordUtils.hashPassword(newPassword);
    runInTransaction(
        conn -> {
          user.getAccount().setPassword(hashed);
          userDAO.update(conn, user);
        });
  }

  public void deposit(int userId, long amount) {
    if (amount <= 0) throw new ServiceException("Số tiền nạp phải > 0");
    runInTransaction(
        conn -> {
          userDAO.lockRow(conn, userId);
          User user =
              userDAO
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          try {
            user.getWallet().deposit(amount);
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
          userDAO.update(conn, user);
        });
  }

  public void withdraw(String username, String password, long amount) {
    User user = login(username, password);
    if (amount <= 0) throw new ServiceException("Số tiền rút phải > 0");
    runInTransaction(
        conn -> {
          userDAO.lockRow(conn, user.getId());
          User stored =
              userDAO
                  .findById(conn, user.getId())
                  .orElseThrow(
                      () -> new ServiceException("Không tìm thấy user với id: " + user.getId()));
          try {
            stored.getWallet().withdraw(amount);
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
          userDAO.update(conn, stored);
        });
  }

  public List<User> getAllUsers() {
    return userDAO.findAll();
  }

  private void validateNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new ServiceException(fieldName + " không được để trống.");
    }
  }

  private void runInTransaction(java.util.function.Consumer<Connection> work) {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
      conn.setAutoCommit(false);
      try {
        work.accept(conn);
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (java.sql.SQLException e) {
      throw new DatabaseException("Lỗi transaction.", e);
    }
  }

  private <T> T runInTransaction(java.util.function.Function<Connection, T> work) {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
      conn.setAutoCommit(false);
      try {
        T result = work.apply(conn);
        conn.commit();
        return result;
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (java.sql.SQLException e) {
      throw new DatabaseException("Lỗi transaction.", e);
    }
  }
}
