package app.data;

/** AuctionDetailResponse. */
public record AuctionDetailResponse(boolean success, String message, AuctionDetail detail)
    implements Response {}
