package app.data;

import java.io.Serializable;

public record PlaceBidRequest(int sessionId, int bidderId, long bidAmount, long currentPrice)
    implements Serializable {}
