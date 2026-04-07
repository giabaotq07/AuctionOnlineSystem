package Common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Item {
  private String itemName;
  private String category;
  private double currentPrice;
  private LocalDateTime endTime;

  public Item(String name, String cat, double price, LocalDateTime end) {
    this.itemName = name;
    this.category = cat;
    this.currentPrice = price;
    this.endTime = end;
  }

  // ===================== GETTER / SETTER =====================
  public String getItemName() {
    return itemName;
  }

  public String getCategory() {
    return category;
  }

  public double getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(double price) {
    this.currentPrice = price;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  // ===================== FORMAT TIME =====================
  public String getFormattedEndTime() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    return endTime.format(formatter);
  }

  // ===================== TO STRING (CHO UI) =====================
  @Override
  public String toString() {
    return "[" + category + "] " + itemName +
            " | Giá: " + currentPrice +
            " | Kết thúc: " + getFormattedEndTime();
  }
}