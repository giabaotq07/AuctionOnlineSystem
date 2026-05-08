package app.dao;

import app.models.User;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
  Optional<User> findById(int id);

  Optional<User> findByUsername(String username);

  List<User> findAll();

  User save(User user);

  void update(User user);

  void adjustWallet(int id, long delta);
}
