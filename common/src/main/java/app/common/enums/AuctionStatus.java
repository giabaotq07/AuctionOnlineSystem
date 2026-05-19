package app.common.enums;

/** AuctionStatus. */
public enum AuctionStatus {
  OPEN(0),
  RUNNING(1),
  ENDING_SOON(2),
  FINISHED(3),
  PAID(4),
  CANCELED(5),
  ;
  private final int state;

  AuctionStatus(int state) {
    this.state = state;
  }

  public int getState() {
    return state;
  }
}
