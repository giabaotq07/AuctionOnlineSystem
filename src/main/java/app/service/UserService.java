package app.service;

import app.dao.UserDAO;
import app.exception.AuthenticationException;
import app.exception.NotFoundException;
import app.exception.UserAlreadyExistsException;
import app.models.User;
import app.util.PasswordUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
  private final UserDAO userDAO;
  private final Map<Integer, User> userCache = new ConcurrentHashMap<>();

  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public User login(String account, String rawPassword) {
    User user = userDAO.findByUsername(account).orElse(null);
    if (user != null && PasswordUtils.verify(rawPassword, user.getAccount().getPassword())) {
      userCache.put(user.getId(), user);
      return user;
    }
    throw new AuthenticationException("Đăng nhập thất bại: " + account);
  }

  public User register(User user) {
    if (userDAO.findByUsername(user.getAccount().getUsername()).isPresent()) {
      throw new UserAlreadyExistsException("User đã tồn tại: " + user.getAccount().getUsername());
    }
    user = userDAO.save(user);
    userCache.put(user.getId(), user);
    return user;
  }

  public User getUserByAccount(String account) {
    User user = userDAO.findByUsername(account).orElse(null);
    if (user == null) {
      throw new NotFoundException("Không tìm thấy user: " + account);
    }
    userCache.put(user.getId(), user);
    return user;
  }

  public User getUserById(int id) {
    User user = userDAO.findById(id).orElse(null);
    if (user == null) {
      throw new NotFoundException("Không tìm thấy user với ID: " + id);
    }
    userCache.put(id, user);
    return user;
  }

  public boolean updateProfile(User user) {
    boolean ok = userDAO.updateProfile(user.getId(), user.getName());
    if (!ok) {
      return false;
    }
    user = userDAO.findById(user.getId()).orElse(null);
    userCache.put(user.getId(), user);
    return true;
  }

  public void changePassword(String username, String oldPassword, String newPassword) {
    User user = login(username, oldPassword);
    boolean ok = userDAO.updatePassword(user.getId(), newPassword);
    if (ok) {
      user.getAccount().setPassword(newPassword);
      userCache.put(user.getId(), user);
    }
  }

  public void changeUsername(String oldUsername, String newUsername, String password) {
    User user = login(oldUsername, password);
    boolean ok = userDAO.updateUsername(user.getId(), newUsername);
    if (ok) {
      user.getAccount().setUsername(newUsername);
      userCache.put(user.getId(), user);
    }
  }

  public boolean deposit(String username, String password, long amount) {
    User user = login(username, password);
    boolean ok = userDAO.adjustWallet(user.getId(), amount);
    if (!ok) {
      return false;
    }
    userCache.put(user.getId(), userDAO.findById(user.getId()).orElse(null));
    return true;
  }

  public boolean withdraw(String username, String password, long amount) {
    User user = login(username, password);
    boolean ok = userDAO.adjustWallet(user.getId(), -amount);
    if (!ok) {
      return false;
    }
    userCache.put(user.getId(), userDAO.findById(user.getId()).orElse(null));
    return true;
  }

  public List<User> getAllUsers() {
    return userDAO.findAll();
  }
}
