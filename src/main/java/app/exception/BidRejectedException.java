package app.exception;

public class BidRejectedException extends RuntimeException {
	public BidRejectedException(String message) {
		super(message);
	}
}
