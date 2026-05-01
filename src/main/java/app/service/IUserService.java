package app.service;

import app.exception.InvalidCredentialsException;
import app.exception.UserAlreadyExistsException;
import app.models.User;
import java.util.List;

public interface IUserService {
  boolean login(String account, String rawPassword) throws InvalidCredentialsException;

  boolean register(User user) throws UserAlreadyExistsException;

  User getUserByAccount(String account);

  User getUserById(int id);

  boolean updateProfile(User user);

  boolean updateWallet(User user);

  boolean deleteUser(int id);

  List<User> getAllUsers();
}
