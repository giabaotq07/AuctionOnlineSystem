package app.obserser;

public interface Observer {
  void onNewBid(long newPrice, int auctionId, int bidderId);

  void onAuctionClosed(long finalPrice, int auctionId, int bidderId);
}
