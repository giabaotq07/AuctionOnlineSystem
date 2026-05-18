package app.dto;

/** PlaceBidResponse. */
public record PlaceBidResponse(long auctionId, long highestBidAmount, long bidderId)
    implements Response {}
