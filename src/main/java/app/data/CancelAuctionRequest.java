package app.data;

public record CancelAuctionRequest(int auctionId, int expectedVersion) implements Request {}
