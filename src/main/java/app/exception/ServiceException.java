package app.exception;

public class ServiceException extends AppException {
  public ServiceException(String message) {
    super(message);
  }

  public ServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
