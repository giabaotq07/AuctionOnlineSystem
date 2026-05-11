package app.data;

public record CreateAuctionResponse(boolean success, String message, AuctionSummary auction) {}
