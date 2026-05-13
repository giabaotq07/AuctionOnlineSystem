package app.data;

public record PlaceBidRequest(int auctionId, int bidderId, long bidAmount, long currentPrice)
    implements Request {}
