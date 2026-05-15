package app.dto;

import app.enums.ItemStatus;
import app.enums.ItemType;
import app.models.Item;

/** ItemData. */
public record ItemData(
    int id,
    int sellerId,
    String name,
    String description,
    long startingPrice,
    long stepPrice,
    ItemType type,
    ItemStatus status) {
  /** ItemData. */
  public ItemData(Item item) {
    this(
        item.getId(),
        item.getSellerId(),
        item.getName(),
        item.getDescription(),
        item.getStartingPrice(),
        item.getStepPrice(),
        item.getType(),
        item.getStatus());
  }
}
