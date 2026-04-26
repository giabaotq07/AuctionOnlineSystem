package app.models;

public class Vehicle extends Item {
  public Vehicle(
      String name, String description, double startingPrice, double stepPrice, ItemType type) {
    super(name, description, startingPrice, stepPrice, type);
  }

  public Vehicle(
      int id,
      String name,
      String description,
      double startingPrice,
      double stepPrice,
      ItemType type) {
    super(id, name, description, startingPrice, stepPrice, type);
  }
}
