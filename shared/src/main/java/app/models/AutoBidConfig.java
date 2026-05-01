package app.models;

public class AutoBidConfig extends Entity {
  private int auctionId;
  private int bidderId;
  private double maxAmount;
  private double increment;
  private boolean active;

  public AutoBidConfig() {
    super();
  }

  public AutoBidConfig(int id, int auctionId, int bidderId, double maxAmount, double increment) {
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

  public double getMaxAmount() {
    return maxAmount;
  }

  public double getIncrement() {
    return increment;
  }

  public boolean isActive() {
    return active;
  }

  public void deactivate() {
    this.active = false;
  }

  public boolean canIncrease(double currentPrice) {
    return active && currentPrice + increment <= maxAmount;
  }

  public double getNextBid(double currentPrice) {
    if (!canIncrease(currentPrice)) {
      return currentPrice;
    }
    return currentPrice + increment;
  }
}

