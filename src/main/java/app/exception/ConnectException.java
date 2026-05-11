package app.exception;

import java.io.Serial;

public class ConnectException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public ConnectException(String message) {
    super(message);
  }

  public ConnectException(String message, Throwable cause) {
    super(message, cause);
  }
}
