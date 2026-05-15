package app.data;

/** PlaceBidResponse. */
public record PlaceBidResponse(
    boolean success, long auctionId, long highestBidAmount, long bidderId, String message)
    implements Response {}
