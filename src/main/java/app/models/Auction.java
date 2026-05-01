package app.models;

import app.enums.AuctionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
  private int version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private List<Integer> activeBidderIds;

  // ==================== Constructors ====================

  // Dùng khi tạo mới (INSERT)
  public Auction(int itemId, int sellerId, LocalDateTime endTime, long depositAmount) {
    this.itemId = itemId;
    this.sellerId = sellerId;
    this.endTime = endTime;
    this.depositAmount = depositAmount;
    this.status = AuctionStatus.OPEN;
    this.highestBid = 0;
    this.extendedCount = 0;
    this.version = 0;
    this.activeBidderIds = new ArrayList<>();
    this.createdAt = LocalDateTime.now();
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
      int extendedCount,
      int version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
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
    this.version = version;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.activeBidderIds = new ArrayList<>();
  }

  public void start() {
    if (this.status != AuctionStatus.OPEN) {
      throw new IllegalStateException("Chỉ có thể bắt đầu phiên đang OPEN, hiện tại: " + status);
    }
    this.status = AuctionStatus.RUNNING;
    this.startTime = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public void finish() {
    if (this.status != AuctionStatus.RUNNING) {
      throw new IllegalStateException(
          "Chỉ có thể kết thúc phiên đang RUNNING, hiện tại: " + status);
    }
    this.status = AuctionStatus.FINISHED;
    this.updatedAt = LocalDateTime.now();
  }

  public void markPaid() {
    if (this.status != AuctionStatus.FINISHED) {
      throw new IllegalStateException("Chỉ có thể PAID khi FINISHED, hiện tại: " + status);
    }
    this.status = AuctionStatus.PAID;
    this.updatedAt = LocalDateTime.now();
  }

  public void cancel() {
    if (this.status == AuctionStatus.FINISHED || this.status == AuctionStatus.PAID) {
      throw new IllegalStateException("Không thể huỷ phiên đã " + status);
    }
    this.status = AuctionStatus.CANCELLED;
    this.updatedAt = LocalDateTime.now();
  }

  public void updateHighestBid(long newBid, int bidderId) {
    if (newBid <= this.highestBid) {
      throw new IllegalArgumentException("Giá mới phải cao hơn giá hiện tại: " + highestBid);
    }
    this.highestBid = newBid;
    this.winnerId = bidderId;
    this.version++; // optimistic locking
    this.updatedAt = LocalDateTime.now();
  }

  /** Anti-sniping: gia hạn thêm extraSeconds nếu bid trong X giây cuối */
  public void extend(int extraSeconds) {
    if (this.status != AuctionStatus.RUNNING) {
      throw new IllegalStateException("Chỉ gia hạn khi đang RUNNING");
    }
    this.endTime = this.endTime.plusSeconds(extraSeconds);
    this.extendedCount++;
    this.updatedAt = LocalDateTime.now();
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.endTime);
  }

  public boolean isRunning() {
    return this.status == AuctionStatus.RUNNING;
  }

  public void addActiveBidder(int userId) {
    if (!activeBidderIds.contains(userId)) {
      activeBidderIds.add(userId);
    }
  }

  public void removeActiveBidder(int userId) {
    activeBidderIds.remove(Integer.valueOf(userId));
  }

  public List<Integer> getActiveBidderIds() {
    return new ArrayList<>(activeBidderIds);
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

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
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
        + ", version="
        + version
        + '}';
  }
}
