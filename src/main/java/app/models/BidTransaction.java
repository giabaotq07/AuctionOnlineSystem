package app.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
  private int id;
  private final int bidderId;
  private final String bidderName;
  private final long amount;
  private final LocalDateTime createAt;
  private boolean isAutoBid;

  public BidTransaction(
      int id,
      int bidderId,
      String bidderName,
      long amount,
      LocalDateTime createAt,
      boolean isAutoBid) {
    this.id = id;
    this.bidderId = bidderId;
    this.bidderName = bidderName;
    this.amount = amount;
    this.createAt = createAt;
    this.isAutoBid = isAutoBid;
  }

  public BidTransaction(int bidderId, String bidderName, long amount, boolean isAutoBid) {
    this.bidderId = bidderId;
    this.bidderName = bidderName;
    this.amount = amount;
    this.createAt = LocalDateTime.now();
    this.isAutoBid = isAutoBid;
  }

  public int getId() {
    return id;
  }

  public String getBidderName() {
    return bidderName;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getBidderId() {
    return bidderId;
  }

  public long getAmount() {
    return amount;
  }

  public LocalDateTime getCreateAt() {
    return createAt;
  }

  @Override
  public String toString() {
    return bidderId + " đã trả $" + amount + " vào lúc " + createAt.withNano(0);
  }

  public boolean isAutoBid() {
    return isAutoBid;
  }

  public void setAutoBid(boolean autoBid) {
    isAutoBid = autoBid;
  }
}
