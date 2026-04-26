package app.exceptions;

public class UserAlreadyExistsException extends ServiceException {
  public UserAlreadyExistsException(String message) {
    super(message);
  }

  public UserAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
