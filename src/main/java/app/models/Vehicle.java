package app.models;

public class Vehicle extends Item {
  public Vehicle(String name, String description, double startingPrice, double stepPrice) {
    super(name, description, startingPrice, stepPrice, ItemType.VEHICLE);
  }
}
