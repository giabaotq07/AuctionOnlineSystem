package app.models;

import app.enums.ItemType;

public class Electronics extends Item {
  public Electronics(
      String name, int sellerId, String description, double startingPrice, double stepPrice) {
    this(0, name, sellerId, description, startingPrice, stepPrice);
  }

  public Electronics(
      int id,
      String name,
      int sellerId,
      String description,
      double startingPrice,
      double stepPrice) {
    super(id, name, sellerId, description, startingPrice, stepPrice, ItemType.ELECTRONICS);
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
