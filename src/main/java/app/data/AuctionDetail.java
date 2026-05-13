package app.data;

import java.time.LocalDateTime;

public record AuctionDetail(
    int auctionId,
    String itemName,
    String description,
    long startingPrice,
    long stepPrice,
    long currentPrice,
    LocalDateTime endTime,
    int version) {}
