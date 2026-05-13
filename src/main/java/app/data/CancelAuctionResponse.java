package app.data;

public record CancelAuctionResponse(boolean success, String message, int auctionId)
    implements Response {}
