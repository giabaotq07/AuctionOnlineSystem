package app.exception;

import java.io.Serial;

public class DatabaseException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public DatabaseException(String message) {
    super(message);
  }

  public DatabaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
