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

public class UserService implements IUserService {
  private final UserDAO userDAO;
  private final Map<String, User> userCache = new ConcurrentHashMap<>();

  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  @Override
  public User login(String account, String rawPassword) throws InvalidCredentialsException {
    User user = userDAO.getUserByAccount(account);
    if (user == null || !PasswordUtils.verify(rawPassword, user.getAccount().getPassword())) {
      throw new InvalidCredentialsException("Tài khoản hoặc mật khẩu không đúng.");
    }
    return user;
  }

  @Override
  public User register(User user) throws UserAlreadyExistsException {
    User exists = userDAO.getUserByAccount(user.getAccount().getUsername());
    if (exists != null) {
      throw new UserAlreadyExistsException("Tài khoản '" + user.getAccount() + "' đã tồn tại!");
    }
    user = userDAO.add(user);
    userCache.put(user.getAccount().getUsername(), user);
    return user;
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
  public User updateProfile(User user) {
    boolean ok = userDAO.updateUserProfile(user);
    if (!ok) {
      throw new UserNotFoundException(
          "Không thể cập nhật. User '" + user.getAccount().getUsername() + "' không tồn tại.");
    }
    userCache.put(user.getAccount().getUsername(), user);
    return user;
  }

  @Override
  public void updateWallet(User user) {
    boolean ok = userDAO.updateUserWallet(user);
    if (!ok) {
      throw new UserNotFoundException(
          "Không tìm thấy user để cập nhật số dư: " + user.getAccount().getUsername());
    }
  }

  @Override
  public void deleteUser(int id) {
    boolean ok = userDAO.delete(id);
    if (!ok) {
      throw new UserNotFoundException("Không thể xóa. User '" + id + "' không tồn tại.");
    }
    userCache.remove(getUserById(id).getAccount().getUsername());
  }

  @Override
  public List<User> getAllUsers() {
    return userDAO.getAll();
  }
}
