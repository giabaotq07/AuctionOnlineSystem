package app.data;

import java.io.Serializable;
import java.time.LocalDateTime;

public record AuctionDetail(
    int auctionId,
    String itemName,
    String description,
    long startingPrice,
    long stepPrice,
    long currentPrice,
    LocalDateTime endTime)
    implements Serializable {}
