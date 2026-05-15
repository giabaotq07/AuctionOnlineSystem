package app.models;

import java.time.LocalDateTime;

/** BidPlacedEvent. */
public record BidPlacedEvent(
    int auctionId,
    int bidderId,
    long amount,
    String bidderName,
    boolean autoBid,
    LocalDateTime bidTime) {}
