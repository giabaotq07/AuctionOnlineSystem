package app.models;

import app.enums.AuctionStatus;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

public class Auction {

  private int id;
  private final int itemId;
  private final int sellerId;
  private Integer winnerId;
  private AuctionStatus status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private long depositAmount;
  private long highestBid;
  private int extendedCount;
  private final ReentrantLock lock = new ReentrantLock();

  public Auction(int itemId, int sellerId, LocalDateTime endTime, long depositAmount) {
    this.itemId = itemId;
    this.sellerId = sellerId;
    this.endTime = endTime;
    this.depositAmount = depositAmount;
    this.status = AuctionStatus.OPEN;
    this.highestBid = 0;
    this.extendedCount = 0;
  }

  public Auction(
      int id,
      int itemId,
      int sellerId,
      Integer winnerId,
      AuctionStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      long depositAmount,
      long highestBid,
      int extendedCount) {
    this.id = id;
    this.itemId = itemId;
    this.sellerId = sellerId;
    this.winnerId = winnerId;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.depositAmount = depositAmount;
    this.highestBid = highestBid;
    this.extendedCount = extendedCount;
  }

  public void start() {
    if (this.status != AuctionStatus.OPEN) {
      throw new IllegalStateException("Chỉ có thể bắt đầu phiên đang OPEN, hiện tại: " + status);
    }
    this.status = AuctionStatus.RUNNING;
    this.startTime = LocalDateTime.now();
  }

  public void finish() {
    if (this.status != AuctionStatus.RUNNING) {
      throw new IllegalStateException(
          "Chỉ có thể kết thúc phiên đang RUNNING, hiện tại: " + status);
    }
    this.status = AuctionStatus.FINISHED;
  }

  public void markPaid() {
    if (this.status != AuctionStatus.FINISHED) {
      throw new IllegalStateException("Chỉ có thể PAID khi FINISHED, hiện tại: " + status);
    }
    this.status = AuctionStatus.PAID;
  }

  public void cancel() {
    if (this.status == AuctionStatus.FINISHED || this.status == AuctionStatus.PAID) {
      throw new IllegalStateException("Không thể huỷ phiên đã " + status);
    }
    this.status = AuctionStatus.CANCELLED;
  }

  public void updateHighestBid(long newBid, int bidderId) {
    if (newBid <= this.highestBid) {
      throw new IllegalArgumentException("Giá mới phải cao hơn giá hiện tại: " + highestBid);
    }
    this.highestBid = newBid;
    this.winnerId = bidderId;
  }

  /** Anti-sniping: gia hạn thêm extraSeconds nếu bid trong X giây cuối */
  public void extend(int extraSeconds) {
    if (this.status != AuctionStatus.RUNNING) {
      throw new IllegalStateException("Chỉ gia hạn khi đang RUNNING");
    }
    this.endTime = this.endTime.plusSeconds(extraSeconds);
    this.extendedCount++;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.endTime);
  }

  public boolean isRunning() {
    return this.status == AuctionStatus.RUNNING;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getItemId() {
    return itemId;
  }

  public int getSellerId() {
    return sellerId;
  }

  public Integer getWinnerId() {
    return winnerId;
  }

  public void setWinnerId(Integer winnerId) {
    this.winnerId = winnerId;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public void setStatus(AuctionStatus status) {
    this.status = status;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public long getDepositAmount() {
    return depositAmount;
  }

  public void setDepositAmount(long depositAmount) {
    this.depositAmount = depositAmount;
  }

  public long getHighestBid() {
    return highestBid;
  }

  public void setHighestBid(long highestBid) {
    this.highestBid = highestBid;
  }

  public int getExtendedCount() {
    return extendedCount;
  }

  public void setExtendedCount(int extendedCount) {
    this.extendedCount = extendedCount;
  }

  public ReentrantLock getLock() {
    return lock;
  }

  @Override
  public String toString() {
    return "Auction{"
        + "id="
        + id
        + ", itemId="
        + itemId
        + ", sellerId="
        + sellerId
        + ", winnerId="
        + winnerId
        + ", status="
        + status
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", highestBid="
        + highestBid
        + ", extendedCount="
        + extendedCount
        + '}';
  }
}
