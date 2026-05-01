package app.models;

import app.enums.ItemType;

public class Item extends Entity {
  protected String name;
  protected String description;
  protected double startingPrice;
  protected double stepPrice;
  protected ItemType type;

  public Item(
      String name, String description, double startingPrice, double stepPrice, ItemType type) {
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.stepPrice = stepPrice;
    this.type = type;
  }

  public Item(
      int id,
      String name,
      String description,
      double startingPrice,
      double stepPrice,
      ItemType type) {
    this.id = id;
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
