package app.config;

import app.exception.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

public class TransactionManager {

  private static final TransactionManager INSTANCE = new TransactionManager();

  private TransactionManager() {}

  public static TransactionManager getInstance() {
    return INSTANCE;
  }

  public <T> T runInTransaction(Function<Connection, T> work) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      conn.setAutoCommit(false);
      try {
        T result = work.apply(conn);
        conn.commit();
        return result;
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi transaction.", e);
    }
  }

  public void runInTransaction(Consumer<Connection> work) {
    runInTransaction(conn -> { work.accept(conn); return null; });
  }
}