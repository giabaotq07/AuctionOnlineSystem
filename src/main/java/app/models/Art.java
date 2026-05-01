package app.models;

import app.enums.ItemType;

public class Art extends Item {
  public Art(
      String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    this(0, name, sellerId, description, startingPrice, stepPrice);
  }

  public Art(
      int id,
      String name,
      int sellerId,
      String description,
      Long startingPrice,
      Long stepPrice) {
    super(id, name, sellerId, description, startingPrice, stepPrice, ItemType.ART);
  }

  @Override
  public void printInfo() {
    System.out.printf(
        """
      === Art Item ===
      ID: %d | Name: %s
      Description: %s
      Starting Price: %.2f | Step: %.2f
      %n""",
        getId(), getName(), getDescription(), getStartingPrice(), getStepPrice());
  }
}
