package app.observer;

public interface AuctionObserver {
  void onNewBidPlaced(String itemName, long newPrice, String bidderName);

  void onAuctionClosed(String itemName, String winnerName, long finalPrice);
}
