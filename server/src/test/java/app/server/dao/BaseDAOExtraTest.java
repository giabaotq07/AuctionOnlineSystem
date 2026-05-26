package app.server.dao;

import static org.junit.jupiter.api.Assertions.*;

import app.common.exception.DatabaseException;
import app.server.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

public class BaseDAOExtraTest extends BaseDAOTest {

  private static class DummyDAO extends BaseDAO {
    public boolean testExecuteUpdate(Connection conn, String sql, Object... params) {
      return executeUpdate(conn, sql, params);
    }

    public <T> T testWithConnection(java.util.function.Function<Connection, T> action) {
      return withConnection(action, "Error action");
    }

    public void testRunWithConnection(java.util.function.Consumer<Connection> action) {
      runWithConnection(action, "Error run");
    }

    public <T> T testRunInTransaction(java.util.function.Function<Connection, T> action) {
      return runInTransaction(action, "Error tx");
    }

    public void testRunInTransactionVoid(java.util.function.Consumer<Connection> action) {
      runInTransaction(action, "Error tx void");
    }
  }

  @Test
  public void testBaseDAOValidations() {
    DummyDAO dao = new DummyDAO();

    // 1. null sql
    assertThrows(DatabaseException.class, () -> dao.testExecuteUpdate(null, null));

    // 2. blank sql
    assertThrows(DatabaseException.class, () -> dao.testExecuteUpdate(null, "   "));

    // 3. invalid update prefix (like SELECT)
    assertThrows(DatabaseException.class, () -> dao.testExecuteUpdate(null, "SELECT * FROM users"));
  }

  @Test
  public void testBaseDAOExceptions() {
    DummyDAO dao = new DummyDAO();

    dao.testRunWithConnection(
        conn -> {
          try {
            conn.close();
          } catch (SQLException e) {
          }
          assertThrows(
              DatabaseException.class,
              () -> dao.testExecuteUpdate(conn, "INSERT INTO users VALUES (1)"));
        });
  }

  @Test
  public void testWithConnectionClosedPool() {
    DummyDAO dao = new DummyDAO();

    // Close the connection pool to force SQLException on getConnection()
    DatabaseConnection.getDataSource().close();
    try {
      assertThrows(DatabaseException.class, () -> dao.testWithConnection(conn -> null));
      assertThrows(DatabaseException.class, () -> dao.testRunWithConnection(conn -> {}));
      assertThrows(DatabaseException.class, () -> dao.testRunInTransaction(conn -> null));
    } finally {
      DatabaseConnection.resetDataSource();
    }
  }

  @Test
  public void testRunInTransactionException() {
    DummyDAO dao = new DummyDAO();
    assertThrows(
        RuntimeException.class,
        () ->
            dao.testRunInTransaction(
                conn -> {
                  throw new RuntimeException("Mock error");
                }));

    // Void tx
    assertThrows(
        RuntimeException.class,
        () ->
            dao.testRunInTransactionVoid(
                conn -> {
                  throw new RuntimeException("Mock error void");
                }));
  }
}
