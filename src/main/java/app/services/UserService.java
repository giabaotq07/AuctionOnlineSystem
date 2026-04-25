package app.services;

import app.dao.UserDAO;
import app.exceptions.UserAlreadyExistsException;
import app.exceptions.UserNotFoundException;
import app.models.User;
import java.util.List;

public class UserService {
  private final UserDAO userDAO;

  // Sử dụng Dependency Injection thông qua Constructor
  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public boolean login(String account, String password) {
    if (account == null || password == null || account.isEmpty()) {
      return false;
    }
    return userDAO.checkLogin(account, password);
  }

  public User register(User user) throws UserAlreadyExistsException {
    // Có thể thêm logic kiểm tra độ dài mật khẩu, định dạng email tại đây
//    if (user.getAccount() == null || user.getAccount().length() < 3) {
//      throw new IllegalArgumentException("Tài khoản phải có ít nhất 3 ký tự.");
//    }
    return userDAO.addUser(user);
  }

  public User getUserByAccount(String account) throws UserNotFoundException {
    return userDAO.loadUsers(account);
  }

  public User getUserById(int id) throws UserNotFoundException {
    return userDAO.getUserById(id);
  }

  public void updateProfile(User user) throws UserNotFoundException {
    // DAO đã ném UserNotFoundException nếu không tìm thấy, nên chỉ cần gọi
    userDAO.updateUser(user);
  }

  public void updateBalance(User user) throws UserNotFoundException {
    userDAO.updateUserBalance(user);
  }

  public void deleteUser(String account) throws UserNotFoundException {
    userDAO.deleteUser(account);
  }

  public List<User> getAllUsers() {
    return userDAO.getAllUsers();
  }

  public String getUserRole(String account) {
    String role = userDAO.getUserRole(account);
    return (role != null) ? role : "GUEST"; // Trả về role mặc định nếu không tìm thấy
  }
}