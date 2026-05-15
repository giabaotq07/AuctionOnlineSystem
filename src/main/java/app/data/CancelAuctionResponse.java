package app.data;

/** CancelAuctionResponse. */
public record CancelAuctionResponse(boolean success, String message, int auctionId)
    implements Response {}
