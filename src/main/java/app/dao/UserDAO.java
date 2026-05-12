package app.dao;

import app.models.User;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
  Optional<User> findById(int id);

  Optional<User> findById(Connection conn, int id);

  Optional<User> findByUsername(String username);

  Optional<User> findByUsername(Connection conn, String username);

  List<User> findAll();

  User save(User user);

  User save(Connection conn, User user);

  void update(User user);

  void update(Connection conn, User user);

  void lockRow(Connection conn, int id);

  void deleteAll();
}
