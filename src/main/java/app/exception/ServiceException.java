package app.exception;

import java.io.Serial;

public class ServiceException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public ServiceException(String message) {
    super(message);
  }

  public ServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
