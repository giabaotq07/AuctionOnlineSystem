package Common;

public interface AuctionSubject {
    void registerObserver(AuctionObserver observer);
    void removeObserver(AuctionObserver observer);
    void notifyObserversNewBid(double price, String bidderName);
}
