package app.common.dto;

import java.time.LocalDateTime;

/** BidData. */
public record BidData(
    int id,
    int auctionId,
    int bidderId,
    String bidderName,
    long amount,
    LocalDateTime createAt,
    boolean autoBid) {}
