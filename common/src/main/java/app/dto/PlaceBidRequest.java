package app.dto;

/** PlaceBidRequest. */
public record PlaceBidRequest(int auctionId, long bidAmount) implements Request {}
