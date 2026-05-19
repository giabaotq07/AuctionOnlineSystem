package app.common.dto;

import app.common.enums.ItemStatus;
import app.common.enums.ItemType;

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
