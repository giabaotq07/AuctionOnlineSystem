package app.models;

import app.enums.ItemType;

public abstract class Item extends Entity {
  protected int sellerId;
  protected String name;
  protected String description;
  protected double startingPrice;
  protected double stepPrice;
  protected ItemType type;

  public Item(
      String name,
      int sellerId,
      String description,
      double startingPrice,
      double stepPrice,
      ItemType type) {
    this(0, name, sellerId, description, startingPrice, stepPrice, type);
  }

  public Item(
      int id,
      String name,
      int sellerId,
      String description,
      double startingPrice,
      double stepPrice,
      ItemType type) {
    super(id);
    this.sellerId = sellerId;
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.stepPrice = stepPrice;
    this.type = type;
  }

  public int getSellerId() {
    return sellerId;
  }

  public void setSellerId(int sellerId) {
    this.sellerId = sellerId;
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

  public void setDescription(String description) {
    this.description = description;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(double startingPrice) {
    this.startingPrice = startingPrice;
  }

  public double getStepPrice() {
    return stepPrice;
  }

  public void setStepPrice(double stepPrice) {
    this.stepPrice = stepPrice;
  }

  public ItemType getType() {
    return type;
  }

  public void setType(ItemType type) {
    this.type = type;
  }

  public abstract void printInfo();
}
