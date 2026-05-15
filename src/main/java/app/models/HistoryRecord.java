package app.models;

import app.enums.HistoryType;
import java.time.LocalDateTime;

/** HistoryRecord. */
public class HistoryRecord {
  private int auctionId;
  private HistoryType type;
  private String message;
  private LocalDateTime time;

  /** HistoryRecord. */
  public HistoryRecord(int auctionId, HistoryType type, String message) {
    this.auctionId = auctionId;
    this.type = type;
    this.message = message;
    this.time = LocalDateTime.now();
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public HistoryType getType() {
    return type;
  }

  public void setType(HistoryType type) {
    this.type = type;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public void setTime(LocalDateTime time) {
    this.time = time;
  }
}
