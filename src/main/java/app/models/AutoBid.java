package app.models;

import java.time.LocalDateTime;

public class AutoBid {
  private int id;
  private int sessionId;
  private int userId;
  private long maxBid;
  private long increment;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public AutoBid(int id, int sessionId, int userId, long maxBid, long increment) {
    this.id = id;
    this.sessionId = sessionId;
    this.userId = userId;
    this.maxBid = maxBid;
    this.increment = increment;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getSessionId() {
    return sessionId;
  }

  public void setSessionId(int sessionId) {
    this.sessionId = sessionId;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public long getMaxBid() {
    return maxBid;
  }

  public void setMaxBid(long maxBid) {
    this.maxBid = maxBid;
  }

  public long getIncrement() {
    return increment;
  }

  public void setIncrement(long increment) {
    this.increment = increment;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
