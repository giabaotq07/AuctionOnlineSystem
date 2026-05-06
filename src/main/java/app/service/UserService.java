package app.service;

import app.dao.UserDAO;
import app.exception.ServiceException;
import app.models.User;
import app.utils.PasswordUtils;
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
    if (userDAO.findByUsername(user.getAccount().getUsername()).isPresent()) {
      throw new ServiceException("User đã tồn tại: " + user.getAccount().getUsername());
    }
    // Hash password in service layer before persisting (single responsibility)
    String hashed = PasswordUtils.hashPassword(user.getAccount().getPassword());
    user.getAccount().setPassword(hashed);
    return userDAO.save(user);
  }

  public void updateProfile(User user) {
    validateNotBlank(user.getName(), "Họ tên");
    User stored =
        userDAO
            .findById(user.getId())
            .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + user.getId()));
    stored.setName(user.getName());
    userDAO.update(stored);
  }

  public void changePassword(String username, String oldPassword, String newPassword) {
    validateNotBlank(newPassword, "Mật khẩu mới");
    User user = login(username, oldPassword);
    String hashed = PasswordUtils.hashPassword(newPassword);
    user.getAccount().setPassword(hashed);
    userDAO.update(user);
  }

  public void deposit(int userId, long amount) {
    if (amount <= 0) throw new ServiceException("Số tiền nạp phải > 0");
    userDAO
        .findById(userId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
    userDAO.adjustWallet(userId, amount);
  }

  public void withdraw(String username, String password, long amount) {
    User user = login(username, password);
    if (amount <= 0) throw new ServiceException("Số tiền rút phải > 0");
    if (user.getWallet().getAssets() < amount) {
      throw new ServiceException("Số dư không đủ để thực hiện giao dịch.");
    }
    userDAO.adjustWallet(user.getId(), -amount);
  }

  public List<User> getAllUsers() {
    return userDAO.findAll();
  }

  private void validateNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new ServiceException(fieldName + " không được để trống.");
    }
  }
}
