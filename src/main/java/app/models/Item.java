package app.models;

import app.enums.ItemStatus;
import app.enums.ItemType;
import java.time.LocalDateTime;

public abstract class Item extends Entity {
  protected int sellerId;
  protected String name;
  protected String description;
  protected long startingPrice;
  protected long stepPrice;
  protected ItemType type;
  protected ItemStatus status;
  protected LocalDateTime createdAt;
  protected LocalDateTime updatedAt;

  public Item(
      String name,
      int sellerId,
      String description,
      long startingPrice,
      long stepPrice,
      ItemType type) {
    this(0, name, sellerId, description, startingPrice, stepPrice, type);
  }

  public Item(
      int id,
      String name,
      int sellerId,
      String description,
      long startingPrice,
      long stepPrice,
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

  public long getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(long startingPrice) {
    this.startingPrice = startingPrice;
  }

  public long getStepPrice() {
    return stepPrice;
  }

  public void setStepPrice(Long stepPrice) {
    this.stepPrice = stepPrice;
  }

  public ItemType getType() {
    return type;
  }

  public ItemStatus getStatus() {
    return status;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void setStatus(ItemStatus status) {
    this.status = status;
  }

  public void setStepPrice(long stepPrice) {
    this.stepPrice = stepPrice;
  }

  public void setType(ItemType type) {
    this.type = type;
  }

  public abstract void printInfo();
}
