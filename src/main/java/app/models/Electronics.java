package app.models;

import app.enums.ItemType;

public class Electronics extends Item {
  public Electronics(
      String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    this(0, name, sellerId, description, startingPrice, stepPrice);
  }

  public Electronics(
      int id, String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    super(id, name, sellerId, description, startingPrice, stepPrice, ItemType.ELECTRONICS);
  }

  @Override
  public void printInfo() {
    System.out.printf(
        """
      === Art Item ===
      ID: %d | Name: %s
      Description: %s
      Starting Price: %d | Step: %d
      %n""",
        getId(), getName(), getDescription(), getStartingPrice(), getStepPrice());
  }
}
