package Common;

import java.time.LocalDateTime;

/**
 * BidTransaction đại diện cho một lượt đặt giá cụ thể. Kế thừa Entity để lấy thuộc tính id và hỗ
 * trợ Serialization.
 */
public class BidTransaction extends Entity {
  private String auctionId; // Cần thiết để định danh phiên đấu giá khi truyền tin
  private String bidderId;
  private double amount;
  private LocalDateTime time;

  // Constructor nhận cả auctionId để đảm bảo tính toàn vẹn dữ liệu
  public BidTransaction(String auctionId, String bidderId, double amount) {
    super(); // Tự động tạo ID giao dịch duy nhất từ lớp Entity
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.amount = amount;
    this.time = LocalDateTime.now();
  }

  // Getters
  public String getAuctionId() {
    return auctionId;
  }

  public String getBidderId() {
    return bidderId;
  }

  public double getAmount() {
    return amount;
  }

  public LocalDateTime getTime() {
    return time;
  }

  /** Override lại phương thức in thông tin của Entity */
  @Override
  public void printInfo() {
    System.out.println(String.format("[%s] User %s bid %.2f", time, bidderId, amount));
  }
}
