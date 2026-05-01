package app.obserser;

public interface AuctionSubject {
  void registerObserver(AuctionObserver observer);

  void removeObserver(AuctionObserver observer);

  void notifyObserversNewBid(long price, String bidderName);
}
