package app.data;

/** CancelAuctionRequest. */
public record CancelAuctionRequest(int auctionId, int expectedVersion) implements Request {}
