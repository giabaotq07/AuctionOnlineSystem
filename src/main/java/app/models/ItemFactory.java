package app.models;

public class ItemFactory {
  public static Item createItem(
      int id,
      String name,
      String description,
      double startingPrice,
      double stepPrice,
      ItemType type) {
    if (type == null) type = ItemType.GENERAL;
    switch (type) {
      case ELECTRONICS:
        return new Electronics(id, name, description, startingPrice, stepPrice, type);
      case ART:
        return new Art(id, name, description, startingPrice, stepPrice, type);
      case VEHICLE:
        return new Vehicle(id, name, description, startingPrice, stepPrice, type);
      default:
        return new Item(id, name, description, startingPrice, stepPrice, type);
    }
  }

  public static Item createItem(
      String name, String description, double startingPrice, double stepPrice, ItemType type) {
    if (type == null) type = ItemType.GENERAL;
    switch (type) {
      case ELECTRONICS:
        return new Electronics(name, description, startingPrice, stepPrice, type);
      case ART:
        return new Art(name, description, startingPrice, stepPrice, type);
      case VEHICLE:
        return new Vehicle(name, description, startingPrice, stepPrice, type);
      default:
        return new Item(name, description, startingPrice, stepPrice, type);
    }
  }
}
