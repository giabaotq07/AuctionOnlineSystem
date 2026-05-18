package app.dto;

/** CancelAuctionRequest. */
public record CancelAuctionRequest(int auctionId, int expectedVersion) implements Request {}
