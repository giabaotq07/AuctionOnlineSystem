package app.data;

public record PlaceBidRequest(int sessionId, int bidderId, long bidAmount, long currentPrice)
    implements Request {}
