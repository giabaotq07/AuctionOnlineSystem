package app.data;

/** PlaceBidRequest. */
public record PlaceBidRequest(int auctionId, int bidderId, long bidAmount, long currentPrice)
    implements Request {}
