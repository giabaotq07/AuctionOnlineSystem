package app.common.dto;

import java.time.LocalDateTime;

public record BidDto(
    int id,
    int auctionId,
    int bidderId,
    String bidderName,
    long amount,
    LocalDateTime createAt,
    boolean isAutoBid,
    UserDto bidder) {}
