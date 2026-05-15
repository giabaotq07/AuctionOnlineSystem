package app.dto;

/** CreateAuctionResponse. */
public record CreateAuctionResponse(boolean success, String message, AuctionSummary auction)
    implements Response {}
