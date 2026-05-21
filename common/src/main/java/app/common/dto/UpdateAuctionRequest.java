package app.common.dto;

import app.common.enums.ItemType;
import java.time.LocalDateTime;

/** UpdateAuctionRequest. */
public record UpdateAuctionRequest(
    int auctionId,
    String name,
    String description,
    long startingPrice,
    long stepPrice,
    ItemType type,
    int durationMinutes,
    LocalDateTime startTime,
    int expectedVersion)
    implements Request {}
