package app.common.dto;

/** DisableAutoBidResponse. */
public record DisableAutoBidResponse(int auctionId, boolean enabled) implements Response {}
