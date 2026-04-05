package Common;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Thêm format cho dễ nhìn

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

  public void printInfo() {
    // Định dạng thời gian theo kiểu: dd/MM/yyyy HH:mm
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    System.out.println("[" + category + "] " + itemName +
            " | Giá hiện tại: " + currentPrice +
            " | Kết thúc lúc: " + endTime.format(formatter));
  }

  public double getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(double price) { this.currentPrice = price; }
  public String getItemName() { return itemName; }
  public LocalDateTime getEndTime() { return endTime; } // Cần hàm này để Bid kiểm tra
}