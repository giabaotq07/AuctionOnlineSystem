package app.models;

public class Vehicle extends Item {
  public Vehicle(String name, String description, double startingPrice, double stepPrice) {
    super(name, description, startingPrice, stepPrice, ItemType.VEHICLE);
  }

  public Vehicle(int id, String name, String description, double startingPrice, double stepPrice) {
    super(id, name, description, startingPrice, stepPrice, ItemType.VEHICLE);
  }
}
