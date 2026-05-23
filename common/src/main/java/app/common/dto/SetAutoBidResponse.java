package app.common.dto;

/** SetAutoBidResponse. */
public record SetAutoBidResponse(
    int auctionId,
    long maxAmount,
    long incrementAmount,
    boolean enabled,
    long highestBid,
    int leadingBidderId)
    implements Response {}
