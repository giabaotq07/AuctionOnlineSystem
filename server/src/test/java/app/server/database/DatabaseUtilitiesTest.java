package app.server.database;

import static org.junit.jupiter.api.Assertions.*;

import app.server.dao.BaseDAOTest;
import org.junit.jupiter.api.Test;

public class DatabaseUtilitiesTest extends BaseDAOTest {

  @Test
  public void testDatabaseConfigGetters() {
    DatabaseConfig config = DatabaseConfig.load();
    assertNotNull(config);
    assertNotNull(config.getUrl());
    assertNotNull(config.getUser());
    assertNotNull(config.getPassword());
  }

  @Test
  public void testDatabaseConnection() {
    assertNotNull(DatabaseConnection.getDataSource());
    assertFalse(DatabaseConnection.getDataSource().isClosed());
  }

  @Test
  public void testTransactionSuccess() {
    TransactionManager tm = new TransactionManager();
    String result =
        tm.runInTransaction(
            conn -> {
              assertNotNull(conn);
              return "Success";
            });
    assertEquals("Success", result);

    tm.runWithoutResult(
        conn -> {
          assertNotNull(conn);
        });
  }

  @Test
  public void testTransactionRollback() {
    TransactionManager tm = new TransactionManager();
    assertThrows(
        RuntimeException.class,
        () ->
            tm.runInTransaction(
                conn -> {
                  throw new RuntimeException("Rollback requested");
                }));
  }
}
