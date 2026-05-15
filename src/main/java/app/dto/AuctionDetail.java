package app.dto;

import java.time.LocalDateTime;

/** AuctionDetail. */
public record AuctionDetail(
    int auctionId,
    String itemName,
    String description,
    long startingPrice,
    long stepPrice,
    long currentPrice,
    LocalDateTime endTime,
    int version) {}
