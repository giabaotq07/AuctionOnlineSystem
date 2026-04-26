package app.models;

public class Electronics extends Item {
  public Electronics(
      String name, String description, double startingPrice, double stepPrice, ItemType type) {
    super(name, description, startingPrice, stepPrice, type);
  }

  public Electronics(
      int id,
      String name,
      String description,
      double startingPrice,
      double stepPrice,
      ItemType type) {
    super(id, name, description, startingPrice, stepPrice, type);
  }
}
