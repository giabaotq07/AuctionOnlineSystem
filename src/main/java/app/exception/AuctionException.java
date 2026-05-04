package app.exception;

public class AuctionException extends ServiceException {
  public AuctionException(String message) {
    super(message);
  }

  public AuctionException(String message, Throwable cause) {
    super(message, cause);
  }
}
