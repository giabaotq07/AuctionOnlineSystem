package app.models;

public class ItemFactory implements java.io.Serializable {
  public static Item createItem(
      int id,
      String name,
      String description,
      double startingPrice,
      double stepPrice,
      ItemType type) {
    return switch (type) {
      case ELECTRONICS -> new Electronics(id, name, description, startingPrice, stepPrice, type);
      case ART -> new Art(id, name, description, startingPrice, stepPrice, type);
      case VEHICLE -> new Vehicle(id, name, description, startingPrice, stepPrice, type);
    };
  }

  public static Item createItem(
      String name, String description, double startingPrice, double stepPrice, ItemType type) {
    return switch (type) {
      case ELECTRONICS -> new Electronics(name, description, startingPrice, stepPrice, type);
      case ART -> new Art(name, description, startingPrice, stepPrice, type);
      case VEHICLE -> new Vehicle(name, description, startingPrice, stepPrice, type);
    };
  }
}
