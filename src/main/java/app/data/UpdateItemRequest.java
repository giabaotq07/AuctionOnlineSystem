package app.data;

import app.enums.ItemType;

public record UpdateItemRequest(
    int itemId, String name, String description, long startingPrice, long stepPrice, ItemType type)
    implements Request {}
