package app.common.dto;

import app.common.enums.ItemType;

public record ItemDto(
    int id,
    int sellerId,
    String name,
    String description,
    long startingPrice,
    long stepPrice,
    ItemType type,
    boolean deleted,
    String imageUrl,
    UserDto seller) {}
