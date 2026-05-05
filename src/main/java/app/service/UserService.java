package app.service;

import app.config.DatabaseConnection;
import app.dao.UserDAO;
import app.exception.DatabaseException;
import app.exception.ServiceException;
import app.models.User;
import app.utils.PasswordUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserService {
  private final UserDAO userDAO;

  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public User login(String username, String rawPassword) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      User user = userDAO.findByUsername(conn, username).orElse(null);
      if (user == null || !PasswordUtils.verify(rawPassword, user.getAccount().getPassword())) {
        throw new ServiceException("Tên đăng nhập hoặc mật khẩu không đúng");
      }
      return user;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi đăng nhập.", e);
    }
  }

  public User register(User user) {
    validateNotBlank(user.getAccount().getUsername(), "Tên đăng nhập");
    validateNotBlank(user.getAccount().getPassword(), "Mật khẩu");
    validateNotBlank(user.getName(), "Họ tên");
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      if (userDAO.findByUsername(conn, user.getAccount().getUsername()).isPresent()) {
        throw new ServiceException("User đã tồn tại: " + user.getAccount().getUsername());
      }
      return userDAO.save(conn, user);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi đăng ký.", e);
    }
  }

  public void updateProfile(User user) {
    validateNotBlank(user.getName(), "Họ tên");
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      userDAO.updateProfile(conn, user.getId(), user.getName());
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi cập nhật hồ sơ.", e);
    }
  }

  public void changePassword(String username, String oldPassword, String newPassword) {
    validateNotBlank(newPassword, "Mật khẩu mới");
    int userId = login(username, oldPassword).getId();
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      userDAO.updatePassword(conn, userId, newPassword);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi đổi mật khẩu.", e);
    }
  }

  public void deposit(int userId, long amount) {
    if (amount <= 0) throw new ServiceException("Số tiền nạp phải > 0");
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      userDAO
          .findById(conn, userId)
          .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
      userDAO.adjustWallet(conn, userId, amount);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi nạp tiền.", e);
    }
  }

  public void withdraw(String username, String password, long amount) {
    User user = login(username, password);
    if (amount <= 0) throw new ServiceException("Số tiền rút phải > 0");
    if (user.getWallet().getAssets() < amount) {
      throw new ServiceException("Số dư không đủ để thực hiện giao dịch.");
    }
    try {
      try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
        userDAO.adjustWallet(conn, user.getId(), -amount);
      }
    } catch (DatabaseException e) {
      if (e.getMessage().contains("Số dư không đủ")) {
        throw new ServiceException("Số dư không đủ để thực hiện giao dịch.");
      }
      throw e;
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi rút tiền.", e);
    }
  }

  public List<User> getAllUsers() {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      return userDAO.findAll(conn);
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi kết nối khi tải danh sách users.", e);
    }
  }

  private void validateNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new ServiceException(fieldName + " không được để trống.");
    }
  }
}
