package app.data;

import app.enums.ItemType;

/** CreateAuctionRequest. */
public record CreateAuctionRequest(
    String name,
    String description,
    long startingPrice,
    long stepPrice,
    ItemType type,
    int durationMinutes,
    int sellerId)
    implements Request {}
