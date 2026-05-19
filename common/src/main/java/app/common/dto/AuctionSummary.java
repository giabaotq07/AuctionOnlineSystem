package app.common.dto;

import app.common.enums.AuctionStatus;
import java.time.LocalDateTime;

/** AuctionSummary. */
public record AuctionSummary(
    int auctionId,
    int itemId,
    int sellerId,
    Integer winnerId,
    AuctionStatus status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    long highestBid,
    int extendedCount,
    int version,
    String itemName,
    long currentPrice) {}
