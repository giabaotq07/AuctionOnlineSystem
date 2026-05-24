package app.common.dto;

/** SetAutoBidRequest. */
public record SetAutoBidRequest(int auctionId, long maxAmount, long incrementAmount)
    implements Request {}
