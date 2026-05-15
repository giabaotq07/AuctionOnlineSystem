package app.dto;

/** AuctionDetailResponse. */
public record AuctionDetailResponse(boolean success, String message, AuctionDetail detail)
    implements Response {}
