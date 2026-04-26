package app.models;

public class Item extends Entity {
  private int id;
  private String name;
  private String description;
  private double startingPrice;
  private double stepPrice;
  private ItemType type;

  public Item(
      String name, String description, double startingPrice, double stepPrice, ItemType type) {
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.stepPrice = stepPrice;
    this.type = type;
  }

  @Override
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public double getStepPrice() {
    return stepPrice;
  }

  public ItemType getType() {
    return type;
  }

  public void setType(ItemType type) {
    this.type = type;
  }

  public double getPrice() {
    return startingPrice;
  }
}
