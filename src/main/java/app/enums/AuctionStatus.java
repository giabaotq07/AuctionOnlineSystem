package app.enums;

import java.io.Serializable;

public enum AuctionStatus implements Serializable {
  OPEN,
  RUNNING,
  ENDING_SOON,
  FINISHED,
  PAID,
  CANCELLED,
}
