package app.data;

import app.enums.ItemType;
import java.io.Serializable;

public record CreateAuctionRequest(
    String name,
    String description,
    long startingPrice,
    long stepPrice,
    ItemType type,
    int durationMinutes,
    int sellerId)
    implements Serializable {}
