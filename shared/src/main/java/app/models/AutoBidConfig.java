package app.models;

public class AutoBidConfig extends Entity {
  private int auctionId;
  private int bidderId;
  private long maxAmount;
  private long increment;
  private boolean active;

  public AutoBidConfig() {
  super();
  }

  public AutoBidConfig(int id, int auctionId, int bidderId, long maxAmount, long increment) {
  super(id);
  this.auctionId = auctionId;
  this.bidderId = bidderId;
  this.maxAmount = maxAmount;
  this.increment = increment;
  this.active = true;
  }

  public int getAuctionId() {
  return auctionId;
  }

  public int getBidderId() {
  return bidderId;
  }

  public long getMaxAmount() {
  return maxAmount;
  }

  public long getIncrement() {
  return increment;
  }

  public boolean isActive() {
  return active;
  }

  public void deactivate() {
  this.active = false;
  }

  public boolean canIncrease(long currentPrice) {
  return active && currentPrice + increment <= maxAmount;
  }

  public long getNextBid(long currentPrice) {
  if (!canIncrease(currentPrice)) {
   return currentPrice;
  }
  return currentPrice + increment;
  }
}

