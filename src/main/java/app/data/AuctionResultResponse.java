package app.data;

public record AuctionResultResponse(
    boolean success, long auctionId, ProfileData winner, long finalPrice) implements Response {}
