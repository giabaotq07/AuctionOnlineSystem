package app.models;

import app.enums.AuctionStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Auction implements AuctionSubject, Serializable {
  private int id;
  private final Item item;
  private final User seller;
  private AuctionStatus status;
  private LocalDateTime endTime;
  private Double bidDeposit;
  private transient List<AuctionObserver> observers = new ArrayList<>();
  private List<BidTransaction> bidTransactionHistory;

  public Auction(Item item, User seller, LocalDateTime endTime) {
    this.item = item;
    this.seller = seller;
    this.endTime = endTime;
    this.status = AuctionStatus.ACTIVE;
    this.bidTransactionHistory = new ArrayList<>();
  }

  public Auction(int id, Item item, User seller, LocalDateTime endTime) {
    this.id = id;
    this.item = item;
    this.seller = seller;
    this.endTime = endTime;
    this.status = AuctionStatus.ACTIVE;
    this.bidTransactionHistory = new ArrayList<>();
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
  public String getFormatEndTime() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    return endTime.format(formatter);
  }
  public void setBidDeposit(Double deposit) {
    this.bidDeposit = deposit;
  }

  public Double getBidDeposit() {
    return bidDeposit;
  }

  public List<BidTransaction> getBidHistory() {
    return bidTransactionHistory;
  }

  public double getCurrentHighestPrice() {
    if (bidTransactionHistory.isEmpty()) return item.getStartingPrice();
    return bidTransactionHistory.getLast().getAmount();
  }

  public synchronized boolean placeBid(User bidder, double bidAmount) {
    if (LocalDateTime.now().isAfter(endTime)) {
      this.status = AuctionStatus.COMPLETED;
      return false;
    }
    if (this.status != AuctionStatus.ACTIVE) return false;
    double minimumRequiredPrice = getCurrentHighestPrice() + item.getStepPrice();
    if (bidAmount < minimumRequiredPrice) return false;
    BidTransaction newBidTransaction = new BidTransaction(bidder, bidAmount, LocalDateTime.now());
    bidTransactionHistory.add(newBidTransaction);
    notifyObserversNewBid(bidAmount, bidder.getName());
    return true;
  }

  @Override
  public String toString() {
    return item.getName() + " | Giá hiện tại: $" + getCurrentHighestPrice();
  }

  public String getItemName() {
    return item.getName();
  }
}
