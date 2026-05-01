package app.service;

import app.util.PasswordUtils;
import app.dao.UserDAO;
import app.exceptions.InvalidCredentialsException;
import app.exceptions.UserAlreadyExistsException;
import app.exceptions.UserNotFoundException;
import app.models.User;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService implements IUserService {
  private final UserDAO userDAO;
  private final Map<String, User> userCache = new ConcurrentHashMap<>();

  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  @Override
  public boolean login(String account, String rawPassword) throws InvalidCredentialsException {
    User user = userDAO.getUserByAccount(account);
      return user != null && PasswordUtils.verify(rawPassword, user.getAccount().getPassword());
  }

  @Override
  public boolean register(User user) throws UserAlreadyExistsException {
    User exists = userDAO.getUserByAccount(user.getAccount().getUsername());
    if (exists != null) {
      return false;
    }
    user = userDAO.add(user);
    userCache.put(user.getAccount().getUsername(), user);
    return true;
  }

  @Override
  public User getUserByAccount(String account) {
    User user = userDAO.getUserByAccount(account);
    if (user == null) {
      throw new UserNotFoundException("Không tìm thấy user: " + account);
    }
    userCache.putIfAbsent(account, user);
    return userCache.get(account);
  }

  @Override
  public User getUserById(int id) {
    User user = userDAO.getById(id);
    if (user == null) {
      throw new UserNotFoundException("Không tìm thấy user với ID: " + id);
    }
    return user;
  }

  @Override
  public boolean updateProfile(User user) {
    boolean ok = userDAO.updateUserProfile(user);
    if (!ok) {
      return false;
    }
    userCache.put(user.getAccount().getUsername(), user);
    return true;
  }

  @Override
  public boolean updateWallet(User user) {
    boolean ok = userDAO.updateUserWallet(user);
    if (!ok) {
      return false;
      }
    userCache.put(user.getAccount().getUsername(), user);
    return true;
  }

  @Override
  public boolean deleteUser(int id) {
    boolean ok = userDAO.delete(id);
    if (!ok) {
      return false;
    }
    userCache.remove(getUserById(id).getAccount().getUsername());
    return true;
  }

  @Override
  public List<User> getAllUsers() {
    return userDAO.getAll();
  }
}
