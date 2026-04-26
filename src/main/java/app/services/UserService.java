package app.services;

import app.config.PasswordUtils;
import app.dao.UserDAO;
import app.exceptions.InvalidCredentialsException;
import app.exceptions.UserAlreadyExistsException;
import app.exceptions.UserNotFoundException;
import app.models.User;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
  private final UserDAO userDAO;
  private final Map<String, User> userCache = new ConcurrentHashMap<>();

  public UserService() {
    this.userDAO = new UserDAO();
  }

  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public User login(String account, String rawPassword) throws InvalidCredentialsException {
    User user = userDAO.getUserByAccount(account);
    if (user == null || !PasswordUtils.verify(rawPassword, user.getAccount().getPassword())) {
      throw new InvalidCredentialsException("Tài khoản hoặc mật khẩu không đúng.");
    }
    return user;
  }

  public User register(User user) throws UserAlreadyExistsException {
    User exists = userDAO.getUserByAccount(user.getAccount().getUsername());
    if (exists != null) {
      throw new UserAlreadyExistsException("Tài khoản '" + user.getAccount() + "' đã tồn tại!");
    }
    user = userDAO.addUser(user);
    userCache.put(user.getAccount().getUsername(), user);
    return user;
  }

  public User getUserByAccount(String account) {
    User user = userDAO.getUserByAccount(account);
    if (user == null) {
      throw new UserNotFoundException("Không tìm thấy user: " + account);
    }
    userCache.putIfAbsent(account, user);
    return userCache.get(account);
  }

  public User getUserById(int id) {
    User user = userDAO.getUserById(id);
    if (user == null) {
      throw new UserNotFoundException("Không tìm thấy user với ID: " + id);
    }
    return user;
  }

  public User updateProfile(User user) {
    boolean ok = userDAO.updateUserProfile(user);
    if (!ok) {
      throw new UserNotFoundException(
          "Không thể cập nhật. User '" + user.getAccount().getUsername() + "' không tồn tại.");
    }
    userCache.put(user.getAccount().getUsername(), user);
    return user;
  }

  public void updateWallet(User user) {
    boolean ok = userDAO.updateUserWallet(user);
    if (!ok) {
      throw new UserNotFoundException(
          "Không tìm thấy user để cập nhật số dư: " + user.getAccount().getUsername());
    }
  }

  public void deleteUser(int id) {
    boolean ok = userDAO.deleteUser(id);
    if (!ok) {
      throw new UserNotFoundException("Không thể xóa. User '" + id + "' không tồn tại.");
    }
    userCache.remove(id);
  }

  public List<User> getAllUsers() {
    return userDAO.getAllUsers();
  }
}
