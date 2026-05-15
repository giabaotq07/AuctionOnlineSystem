package app.dto;

/** CancelAuctionResponse. */
public record CancelAuctionResponse(boolean success, String message, int auctionId)
    implements Response {}
