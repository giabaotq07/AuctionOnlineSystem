package app.data;

public record PlaceBidResponse(
    int bidderId, long highestBidAmount, String itemName, String bidderName) {}
