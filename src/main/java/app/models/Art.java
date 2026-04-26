package app.models;

public class Art extends Item {
  public Art(String name, String description, double startingPrice, double stepPrice) {
    super(name, description, startingPrice, stepPrice, ItemType.ART);
  }
}
