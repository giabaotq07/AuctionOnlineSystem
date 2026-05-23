package app.common.exception;

import java.io.Serial;

/** ValidationException. */
public class ValidationException extends ServiceException {
  /** ValidationException. */
  @Serial private static final long serialVersionUID = 1L;

  /** ValidationException. */
  public ValidationException(String message) {
    super(message);
  }

  /** ValidationException. */
  public ValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
