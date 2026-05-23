package app.common.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** ExceptionsTest. Kiem thu cac lop ngoai le cua he thong. */
public class ExceptionsTest {

  @Test
  public void testConnectException() {
    ConnectException ex1 = new ConnectException("Test message");
    assertEquals("Test message", ex1.getMessage());

    Throwable cause = new RuntimeException("cause");
    ConnectException ex2 = new ConnectException("Test message with cause", cause);
    assertEquals("Test message with cause", ex2.getMessage());
    assertEquals(cause, ex2.getCause());
  }

  @Test
  public void testDatabaseException() {
    DatabaseException ex1 = new DatabaseException("DB error");
    assertEquals("DB error", ex1.getMessage());

    Throwable cause = new RuntimeException("cause");
    DatabaseException ex2 = new DatabaseException("DB error with cause", cause);
    assertEquals("DB error with cause", ex2.getMessage());
    assertEquals(cause, ex2.getCause());
  }

  @Test
  public void testServiceException() {
    ServiceException ex1 = new ServiceException("Service error");
    assertEquals("Service error", ex1.getMessage());

    Throwable cause = new RuntimeException("cause");
    ServiceException ex2 = new ServiceException("Service error with cause", cause);
    assertEquals("Service error with cause", ex2.getMessage());
    assertEquals(cause, ex2.getCause());
  }

  @Test
  public void testAppException() {
    AppException ex1 = new AppException("App error");
    assertEquals("App error", ex1.getMessage());

    Throwable cause = new RuntimeException("cause");
    AppException ex2 = new AppException("App error with cause", cause);
    assertEquals("App error with cause", ex2.getMessage());
    assertEquals(cause, ex2.getCause());
  }
}
