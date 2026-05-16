package app.dto;

import app.enums.ItemStatus;
import app.enums.ItemType;

/** ItemData. */
public record ItemData(
    int id,
    int sellerId,
    String name,
    String description,
    long startingPrice,
    long stepPrice,
    ItemType type,
    ItemStatus status) {}
