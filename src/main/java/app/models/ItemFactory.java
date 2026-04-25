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
        return id > 0
            ? new Electronics(id, name, description, startingPrice, stepPrice)
            : new Electronics(name, description, startingPrice, stepPrice);
      case ART:
        return id > 0
            ? new Art(id, name, description, startingPrice, stepPrice)
            : new Art(name, description, startingPrice, stepPrice);
      case VEHICLE:
        return id > 0
            ? new Vehicle(id, name, description, startingPrice, stepPrice)
            : new Vehicle(name, description, startingPrice, stepPrice);
      default:
        return id > 0
            ? new Item(id, name, description, startingPrice, stepPrice, type)
            : new Item(name, description, startingPrice, stepPrice, type);
    }
  }
}
