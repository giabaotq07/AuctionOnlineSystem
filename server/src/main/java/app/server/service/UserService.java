package app.server.service;

import app.common.exception.ServiceException;
import app.common.models.User;
import app.server.dao.UserDAO;
import app.server.database.TransactionManager;
import app.server.utils.PasswordUtils;
import java.math.BigDecimal;
import java.util.List;

/** UserService. */
public class UserService {
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;

  /** UserService. */
  public UserService(UserDAO userDAO, TransactionManager transactionManager) {
    this.userDAO = userDAO;
    this.transactionManager = transactionManager;
  }

  /** login. */
  public User login(String username, String rawPassword) {
    User user = userDAO.findByUsername(username).orElse(null);
    if (user == null || !PasswordUtils.verify(rawPassword, user.getAccount().getPassword())) {
      throw new ServiceException("Tên đăng nhập hoặc mật khẩu không đúng");
    }
    if (user.isBanned()) {
      throw new ServiceException("Tài khoản đã bị cấm.");
    }
    return user;
  }

  /** register. */
  public User register(User user) {

    return transactionManager.runInTransaction(
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

  /** updateProfile. */
  public void updateProfile(User user) {

    transactionManager.runWithoutResult(
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

  /** changePassword. */
  public void changePassword(String username, String oldPassword, String newPassword) {

    User user = login(username, oldPassword);
    String hashed = PasswordUtils.hashPassword(newPassword);
    transactionManager.runWithoutResult(
        conn -> {
          user.getAccount().setPassword(hashed);
          userDAO.update(conn, user);
        });
  }

  /** getById. */
  public User getById(int userId) {
    return userDAO
        .findById(userId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
  }

  /** getAllUsers. */
  public List<User> getAllUsers(int requesterId) {
    return userDAO.findAll();
  }

  /** deposit. */
  public User deposit(int userId, BigDecimal amount) {

    return transactionManager.runInTransaction(
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
          return user;
        });
  }

  /** updateAvatarUrl. */
  public String updateAvatarUrl(int userId, String avatarUrl) {
    return transactionManager.runInTransaction(
        conn -> {
          User user =
              userDAO
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          String oldAvatarUrl = user.getAvatarUrl();
          user.setAvatarUrl(avatarUrl);
          userDAO.update(conn, user);
          return oldAvatarUrl;
        });
  }

  public void banUser(int userId) {
    transactionManager.runWithoutResult(
        conn -> {
          userDAO.lockRow(conn, userId);
          User user =
              userDAO
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          user.getWallet().clearFrozenFunds();
          user.ban();
          userDAO.update(conn, user);
        });
  }

  public void unbanUser(int userId) {
    transactionManager.runWithoutResult(
        conn -> {
          userDAO.lockRow(conn, userId);
          User user =
              userDAO
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          user.unban();
          userDAO.update(conn, user);
        });
  }
}
