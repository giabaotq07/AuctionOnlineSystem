package app.obserser;

public interface Observer {
  void onNewBid(double newPrice, int auctionId, int bidderId);

  void onAuctionClosed(double finalPrice, int auctionId, int bidderId);
}
