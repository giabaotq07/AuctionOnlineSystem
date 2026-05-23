package app.common.dto;

import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import java.time.LocalDateTime;

/** Lightweight auction projection for list and history screens. */
public record AuctionPreview(
    int auctionId,
    int itemId,
    String itemName,
    String imageUrl,
    ItemType itemType,
    AuctionStatus status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    long highestBid,
    long startingPrice,
    long stepPrice,
    int version,
    UserPreview seller) {}
