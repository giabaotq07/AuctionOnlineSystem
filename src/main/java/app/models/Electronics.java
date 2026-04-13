package app.models;

import java.time.LocalDateTime;

public class Electronics extends Item {
  private int warrantyMonths;

  public Electronics(
      String itemName,
      String description,
      double startingPrice,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String sellerId,
      int warrantyMonths) {
    super(itemName, description, startingPrice, startTime, endTime, sellerId);
    this.warrantyMonths = warrantyMonths;
  }

  public String getCategory() {
    return "Electronics";
  }

  public int getWarrantyMonths() {
    return warrantyMonths;
  }

  public void setWarrantyMonths(int warrantyMonths) {
    this.warrantyMonths = warrantyMonths;
  }

  @Override
  public void printInfo() {
    super.printInfo(); // Gọi lại hàm in của Item [cite: 121]
  }
}
