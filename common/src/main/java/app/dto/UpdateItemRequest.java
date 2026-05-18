package app.dto;

import app.enums.ItemType;

/** UpdateItemRequest. */
public record UpdateItemRequest(
    int itemId, String name, String description, long startingPrice, long stepPrice, ItemType type)
    implements Request {}
