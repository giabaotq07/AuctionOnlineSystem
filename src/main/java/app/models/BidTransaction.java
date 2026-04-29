package app.models;

public class BidTransaction implements java.io.Serializable {
  private int userId, auctionId, amount;

  public BidTransaction(int amount, int auctionId, int userId) {
    this.amount = amount;
    this.auctionId = auctionId;
    this.userId = userId;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }
}
