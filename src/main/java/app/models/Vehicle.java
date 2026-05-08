package app.models;

import app.enums.ItemType;

public class Vehicle extends Item {
  public Vehicle(
      String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    this(0, name, sellerId, description, startingPrice, stepPrice);
  }

  public Vehicle(
      int id, String name, int sellerId, String description, Long startingPrice, Long stepPrice) {
    super(id, name, sellerId, description, startingPrice, stepPrice, ItemType.VEHICLE);
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
