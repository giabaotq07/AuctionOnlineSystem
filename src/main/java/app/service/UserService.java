package app.service;

import app.dao.UserDAO;
import app.exception.AuthenticationException;
import app.exception.ServiceException;
import app.exception.UserAlreadyExistsException;
import app.models.User;
import app.util.PasswordUtils;
import java.util.List;

public class UserService {
  private final UserDAO userDAO;

  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public User login(String username, String rawPassword) {
    User user = userDAO.findByUsername(username).orElse(null);
    if (user == null || !PasswordUtils.verify(rawPassword, user.getAccount().getPassword())) {
      throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu không đúng");
    }
    return user;
  }

  public User register(User user) {
    validateNotBlank(user.getAccount().getUsername(), "Tên đăng nhập");
    validateNotBlank(user.getAccount().getPassword(), "Mật khẩu");
    validateNotBlank(user.getName(), "Họ tên");

    if (userDAO.findByUsername(user.getAccount().getUsername()).isPresent()) {
      throw new UserAlreadyExistsException("User đã tồn tại: " + user.getAccount().getUsername());
    }
    return userDAO.save(user);
  }

  public void updateProfile(User user) {
    userDAO.updateProfile(user.getId(), user.getName());
  }

  public void changePassword(String username, String oldPassword, String newPassword) {
    validateNotBlank(newPassword, "Mật khẩu mới");
    userDAO.updatePassword(login(username, oldPassword).getId(), newPassword);
  }

  public void deposit(int userId, long amount) {
    if (amount <= 0) throw new ServiceException("Số tiền nạp phải > 0");
    userDAO.adjustWallet(userId, amount);
  }

  public void withdraw(String username, String password, long amount) {
    User user = login(username, password);
    if (amount <= 0) throw new ServiceException("Số tiền rút phải > 0");
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
