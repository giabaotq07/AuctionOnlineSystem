package app.service;

import app.dao.UserDAO;
import app.exception.InvalidCredentialsException;
import app.exception.NotFoundException;
import app.exception.UserAlreadyExistsException;
import app.models.User;
import app.util.PasswordUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
  private final UserDAO userDAO;
  private final Map<String, User> userCache = new ConcurrentHashMap<>();

  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public boolean login(String account, String rawPassword) throws InvalidCredentialsException {
    User user = userDAO.findByUsername(account).orElse(null);
    return user != null && PasswordUtils.verify(rawPassword, user.getAccount().getPassword());
  }

  public boolean register(User user) throws UserAlreadyExistsException {
    User exists = userDAO.findByUsername(user.getAccount().getUsername()).orElse(null);
    if (exists != null) {
      return false;
    }
    user = userDAO.save(user);
    userCache.put(user.getAccount().getUsername(), user);
    return true;
  }

  public User getUserByAccount(String account) {
    User user = userDAO.findByUsername(account).orElse(null);
    if (user == null) {
      throw new NotFoundException("Không tìm thấy user: " + account);
    }
    userCache.putIfAbsent(account, user);
    return userCache.get(account);
  }

  public User getUserById(int id) {
    User user = userDAO.findById(id).orElse(null);
    if (user == null) {
      throw new NotFoundException("Không tìm thấy user với ID: " + id);
    }
    return user;
  }

  public boolean updateProfile(User user) {
    boolean ok = userDAO.updateProfile(user.getId(), user.getName());
    if (!ok) {
      return false;
    }
    userCache.put(user.getAccount().getUsername(), user);
    return true;
  }

  public boolean deposit(User user, long amount) {
    boolean ok = userDAO.adjustWallet(user.getId(), amount);
    if (!ok) {
      return false;
    }
    String username = user.getAccount().getUsername();
    userCache.put(username, userDAO.findById(user.getId()).orElse(null));
    return true;
  }

  public boolean withdraw(User user, long amount) {
    boolean ok = userDAO.adjustWallet(user.getId(), -amount);
    if (!ok) {
      return false;
    }
    String username = user.getAccount().getUsername();
    userCache.put(username, userDAO.findById(user.getId()).orElse(null));
    return true;
  }

  public boolean deleteUser(int id) {
    boolean ok = userDAO.delete(id);
    if (!ok) {
      return false;
    }
    userCache.remove(getUserById(id).getAccount().getUsername());
    return true;
  }

  public List<User> getAllUsers() {
    return userDAO.findAll();
  }
}
