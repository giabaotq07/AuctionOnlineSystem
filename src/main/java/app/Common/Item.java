package app.Common;

public class Item {
  private String itemId;
  private String name;
  private String description;
  private double startingPrice; // Giá khởi điểm
  private double stepPrice; // Bước giá (ví dụ: mỗi lần tăng ít nhất 50k)

  public Item(
      String itemId, String name, String description, double startingPrice, double stepPrice) {
    this.itemId = itemId;
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.stepPrice = stepPrice;
  }

  public String getItemId() {
    return itemId;
  }

  public String getName() {
    return name;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public double getStepPrice() {
    return stepPrice;
  }

  public String getDescription() {
    return description;
  }

  public double getPrice() {
    return startingPrice;
  }
}
