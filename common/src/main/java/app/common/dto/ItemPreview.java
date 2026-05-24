package app.common.dto;

import app.common.enums.ItemType;

/** Item projection used by lightweight screens. */
public record ItemPreview(
    int itemId,
    String name,
    String imageUrl,
    ItemType itemType,
    long startingPrice,
    long stepPrice) {}
