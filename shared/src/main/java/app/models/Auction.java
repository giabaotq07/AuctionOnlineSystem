package app.models;

import app.enums.AuctionStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Auction extends Entity {
  private Item item;
  private int sellerId;
  private AuctionStatus status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private BidTransaction highestBid;
  private final List<BidTransaction> bidHistory = new ArrayList<>();

  public Auction() {
    super();
    this.status = AuctionStatus.SCHEDULED;
  }

  public Auction(
      int id,
      Item item,
      int sellerId,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    super(id);
    this.item = item;
    this.sellerId = sellerId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = AuctionStatus.SCHEDULED;
  }

  public Item getItem() {
    return item;
  }

  public int getSellerId() {
    return sellerId;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public void setStatus(AuctionStatus status) {
    this.status = status;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public BidTransaction getHighestBid() {
    return highestBid;
  }

  public List<BidTransaction> getBidHistory() {
    return Collections.unmodifiableList(bidHistory);
  }

  public boolean isRunning() {
    return status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING;
  }

  public boolean isEnded() {
    return status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED;
  }

  public void startAuction() {
    status = AuctionStatus.OPEN;
  }

  public void finishAuction() {
    status = AuctionStatus.FINISHED;
  }

  public void extendAuction(int seconds) {
    if (endTime != null) {
      endTime = endTime.plusSeconds(seconds);
    }
  }

  public boolean validateBid(double amount) {
    double minBid = bidHistory.isEmpty() ? item.getStartPrice() : highestBid.getAmount() + item.getStepPrice();
    return amount >= minBid;
  }

  public boolean placeBid(BidTransaction bid) {
    if (!isRunning() || !validateBid(bid.getAmount())) {
      return false;
    }
    highestBid = bid;
    bidHistory.add(bid);
    status = AuctionStatus.RUNNING;
    return true;
  }

  public long getRemainingSeconds() {
    if (endTime == null) {
      return 0;
    }
    return Math.max(0, Duration.between(LocalDateTime.now(), endTime).getSeconds());
  }
}
