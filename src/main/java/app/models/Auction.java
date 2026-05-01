package app.models;

import app.enums.AuctionStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction implements AuctionSubject, Serializable {
  private int id;
  private final Item item;
  private final User seller;
  private User winner;
  private AuctionStatus status;
  private final LocalDateTime startTime;
  private LocalDateTime endTime;
  private double highestBid;
  private transient List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
  private List<BidTransaction> bidHistory;

  public Auction(
      int id,
      Item item,
      User seller,
      User winner,
      AuctionStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      double highestBid) {

    this.id = id;
    this.item = item;
    this.seller = seller;
    this.winner = winner;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.highestBid = highestBid;
  }

  public Auction(Item item, User seller, LocalDateTime endTime) {
    this.item = item;
    this.seller = seller;
    this.startTime = LocalDateTime.now();
    this.endTime = endTime;
    this.status = AuctionStatus.OPEN;
    this.highestBid = item.getStartingPrice();
  }

  @Override
  public void registerObserver(AuctionObserver observer) {
    if (!observers.contains(observer)) {
      observers.add(observer);
    }
  }

  @Override
  public void removeObserver(AuctionObserver observer) {
    observers.remove(observer);
  }

  @Override
  public void notifyObserversNewBid(double price, String bidderName) {
    for (AuctionObserver observer : observers) {
      observer.onNewBidPlaced(item.getName(), price, bidderName);
    }
  }

  public synchronized boolean addBid(BidTransaction bidTransaction) {
    if (!isBiddable()) return false;

    double minimumBid = highestBid + item.getStepPrice();

    if (bidTransaction.getAmount() < minimumBid) return false;

    bidHistory.add(bidTransaction);

    highestBid = bidTransaction.getAmount();

    winner = bidTransaction.getBidder();

    notifyObserversNewBid(highestBid, winner.getName());
    return true;
  }

  public boolean isBiddable() {
    if (status != AuctionStatus.OPEN) {
      return false;
    }
    return LocalDateTime.now().isBefore(endTime);
  }

  public void finishAuction() {
    this.status = AuctionStatus.FINISHED;
  }

  public int getTotalBids() {
    return bidHistory.size();
  }

  public double getCurrentHighestPrice() {
    return highestBid;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Item getItem() {
    return item;
  }

  public User getSeller() {
    return seller;
  }

  public User getWinner() {
    return winner;
  }

  public void setWinner(User winner) {
    this.winner = winner;
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

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public double getHighestBid() {
    return highestBid;
  }

  public void setHighestBid(double highestBid) {
    this.highestBid = highestBid;
  }

  public List<AuctionObserver> getObservers() {
    return observers;
  }

  public void setObservers(List<AuctionObserver> observers) {
    this.observers = observers;
  }

  public List<BidTransaction> getBidHistory() {
    return bidHistory;
  }

  public void setBidHistory(List<BidTransaction> bidHistory) {
    this.bidHistory = bidHistory;
  }
}
