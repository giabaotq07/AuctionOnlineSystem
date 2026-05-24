package app.common.dto;

/** AuctionResultResponse. */
public record AuctionResultResponse(long auctionId, UserDto winner, long finalPrice)
    implements Response {}
