package app.models;

import app.enums.AuctionStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionSession implements AuctionSubject, Serializable {
  private int id;
  private Item item;
  private User seller;
  private AuctionStatus status;
  private LocalDateTime endTime;
  private Double biddeposit;
  private transient List<AuctionObserver> observers = new ArrayList<>();
  private List<Bid> bidHistory;

  public AuctionSession(Item item, User seller, LocalDateTime endTime) {
    this.item = item;
    this.seller = seller;
    this.endTime = endTime;
    this.status = AuctionStatus.ACTIVE;
    this.bidHistory = new ArrayList<>();
  }

  public AuctionSession(int id, Item item, User seller, LocalDateTime endTime) {
    this.id = id;
    this.item = item;
    this.seller = seller;
    this.endTime = endTime;
    this.status = AuctionStatus.ACTIVE;
    this.bidHistory = new ArrayList<>();
  }

  @Override
  public void registerObserver(AuctionObserver observer) {
    if (observers == null) observers = new ArrayList<>();
    if (!observers.contains(observer)) observers.add(observer);
  }

  @Override
  public void removeObserver(AuctionObserver observer) {
    if (observers != null) {
      observers.remove(observer);
    }
  }

  @Override
  public void notifyObserversNewBid(double price, String bidderName) {
    if (observers != null) {
      for (AuctionObserver observer : observers) {
        observer.onNewBidPlaced(item.getName(), price, bidderName);
      }
    }
  }

  public int getId() {
    return this.id;
  }

  public String getSessionId() {
    return String.valueOf(this.id);
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setStatus(AuctionStatus status) {
    this.status = status;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public Item getItem() {
    return this.item;
  }

  public User getSeller() {
    return this.seller;
  }

  public LocalDateTime getEndTime() {
    return this.endTime;
  }

  public void setBiddeposit(Double deposit) {
    this.biddeposit = deposit;
  }

  public Double getBiddeposit() {
    return biddeposit;
  }

  public List<Bid> getBidHistory() {
    return bidHistory;
  }

  public double getCurrentHighestPrice() {
    if (bidHistory.isEmpty()) return item.getStartingPrice();
    return bidHistory.get(bidHistory.size() - 1).getAmount();
  }

  public synchronized boolean placeBid(User bidder, double bidAmount) {
    if (LocalDateTime.now().isAfter(endTime)) {
      this.status = AuctionStatus.COMPLETED;
      return false;
    }
    if (this.status != AuctionStatus.ACTIVE) return false;
    double minimumRequiredPrice = getCurrentHighestPrice() + item.getStepPrice();
    if (bidAmount < minimumRequiredPrice) return false;
    Bid newBid = new Bid(bidder, bidAmount, LocalDateTime.now());
    bidHistory.add(newBid);
    notifyObserversNewBid(bidAmount, bidder.getName());
    return true;
  }

  @Override
  public String toString() {
    return item.getName() + " | Giá hiện tại: $" + getCurrentHighestPrice();
  }

  public String getItemname() {
    return item.getName();
  }
}
