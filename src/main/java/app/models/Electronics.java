package app.models;

public class Electronics extends Item {
  public Electronics(String name, String description, double startingPrice, double stepPrice) {
    super(name, description, startingPrice, stepPrice, ItemType.ELECTRONICS);
  }
}
