package app.Common;

public interface AuctionObserver {
  void onNewBidPlaced(String itemName, double newPrice, String bidderName);

  void onAuctionClosed(String itemName, String winnerName, double finalPrice);
}
