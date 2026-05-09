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

  // Dùng khi cần trả về kết quả (SELECT, INSERT lấy ID, ...)
  public <T> T runInTransaction(Function<Connection, T> work) {
    try (Connection conn = DatabaseConnection.getConnection()) {
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

  // Dùng khi không cần trả về (INSERT, UPDATE, DELETE thuần)
  public void runInTransaction(Consumer<Connection> work) {
    runInTransaction(
        conn -> {
          work.accept(conn);
          return null;
        });
  }
}
