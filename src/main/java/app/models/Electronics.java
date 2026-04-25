package app.models;

public class Electronics extends Item {
  public Electronics(String name, String description, double startingPrice, double stepPrice) {
    super(name, description, startingPrice, stepPrice, ItemType.ELECTRONICS);
  }

  public Electronics(
      int id, String name, String description, double startingPrice, double stepPrice) {
    super(id, name, description, startingPrice, stepPrice, ItemType.ELECTRONICS);
  }
}
