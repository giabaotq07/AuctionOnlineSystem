package app.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
  private int id;
  private final User bidder;
  private final double amount;
  private final LocalDateTime createAt;

  public BidTransaction(User bidder, double amount) {
    this.bidder = bidder;
    this.amount = amount;
    this.createAt = LocalDateTime.now();
  }

  public BidTransaction(int id, User bidder, double amount) {
    this.id = id;
    this.bidder = bidder;
    this.amount = amount;
    this.createAt = LocalDateTime.now();
  }

  public BidTransaction(int id, User bidder, double amount, LocalDateTime createAt) {
    this.id = id;
    this.bidder = bidder;
    this.amount = amount;
    this.createAt = createAt;
  }

  public BidTransaction(User bidder, double amount, LocalDateTime createAt) {
    this.bidder = bidder;
    this.amount = amount;
    this.createAt = createAt;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public User getBidder() {
    return bidder;
  }

  public double getAmount() {
    return amount;
  }

  public LocalDateTime getCreateAt() {
    return createAt;
  }

  @Override
  public String toString() {
    return bidder.getName() + " đã trả $" + amount + " vào lúc " + createAt.withNano(0);
  }
}
