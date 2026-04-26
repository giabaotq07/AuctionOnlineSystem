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
    User user = userDAO.loadUsers(account);
    if (user == null || !PasswordUtils.verify(rawPassword, user.getPassword())) {
      throw new InvalidCredentialsException("Tài khoản hoặc mật khẩu không đúng.");
    }
    return user;
  }

  public User register(User user) throws UserAlreadyExistsException {
    User exists = userDAO.loadUsers(user.getAccount());
    if (exists != null) {
      throw new UserAlreadyExistsException("Tài khoản '" + user.getAccount() + "' đã tồn tại!");
    }
    user = userDAO.addUser(user);
    userCache.put(user.getAccount(), user);
    return user;
  }

  public User getUserByAccount(String account) {
    User user = userDAO.loadUsers(account);
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
    boolean ok = userDAO.updateUser(user);
    if (!ok) {
      throw new UserNotFoundException(
          "Không thể cập nhật. User '" + user.getAccount() + "' không tồn tại.");
    }
    userCache.put(user.getAccount(), user);
    return user;
  }

  public boolean updateBalance(User user) {
    boolean ok = userDAO.updateUserBalance(user);
    if (!ok) {
      throw new UserNotFoundException(
          "Không tìm thấy user để cập nhật số dư: " + user.getAccount());
    }
  }

  public void deleteUser(String account) {
    boolean ok = userDAO.deleteUser(account);
    if (!ok) {
      throw new UserNotFoundException("Không thể xóa. User '" + account + "' không tồn tại.");
    }
    userCache.remove(account);
  }

  public List<User> getAllUsers() {
    return userDAO.getAllUsers();
  }

  public String getUserRole(String account) {
    String role = userDAO.getUserRole(account);
    return (role != null) ? role : "GUEST";
  }
}
