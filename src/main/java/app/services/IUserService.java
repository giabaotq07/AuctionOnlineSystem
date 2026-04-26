package app.services;

import app.exceptions.InvalidCredentialsException;
import app.exceptions.UserAlreadyExistsException;
import app.models.User;
import java.util.List;

public interface IUserService {
  User login(String account, String rawPassword) throws InvalidCredentialsException;

  User register(User user) throws UserAlreadyExistsException;

  User getUserByAccount(String account);

  User getUserById(int id);

  User updateProfile(User user);

  void updateWallet(User user);

  void deleteUser(int id);

  List<User> getAllUsers();
}
