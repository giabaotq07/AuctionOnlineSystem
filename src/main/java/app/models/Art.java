package app.models;

public class Art extends Item {
  public Art(
      String name, String description, double startingPrice, double stepPrice, ItemType type) {
    super(name, description, startingPrice, stepPrice, type);
  }

  public Art(
      int id,
      String name,
      String description,
      double startingPrice,
      double stepPrice,
      ItemType type) {
    super(id, name, description, startingPrice, stepPrice, type);
  }
}
