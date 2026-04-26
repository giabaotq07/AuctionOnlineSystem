package app.models;

public class ItemFactory {
  public static Item createItem(
      ItemType type, String name, String description, double startingPrice, double stepPrice) {
    return createItemWithId(0, type, name, description, startingPrice, stepPrice);
  }

  public static Item createItemWithId(
      int id,
      ItemType type,
      String name,
      String description,
      double startingPrice,
      double stepPrice) {
    if (type == null) type = ItemType.GENERAL;
    switch (type) {
      case ELECTRONICS:
        return new Electronics(name, description, startingPrice, stepPrice);
      case ART:
        return new Art(name, description, startingPrice, stepPrice);
      case VEHICLE:
        return new Vehicle(name, description, startingPrice, stepPrice);
      default:
        return new Item(name, description, startingPrice, stepPrice, type);
    }
  }
}
