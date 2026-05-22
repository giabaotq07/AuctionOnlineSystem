package app.common.dto;

import app.common.enums.ItemType;
import app.common.models.Item;

/** Item projection used by lightweight screens. */
public record ItemPreview(
    int itemId,
    String name,
    String imageUrl,
    ItemType itemType,
    long startingPrice,
    long stepPrice) {
  public static ItemPreview from(Item item) {
    if (item == null) {
      return null;
    }
    return new ItemPreview(
        item.getId(),
        item.getName(),
        item.getImageUrl(),
        item.getType(),
        item.getStartingPrice(),
        item.getStepPrice());
  }
}
