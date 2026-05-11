package app.data;

public record AuctionResultResponse(
    boolean success, String message, String winnerName, long finalPrice) {}
