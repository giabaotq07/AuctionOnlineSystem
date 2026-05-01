package app.models;

import app.enums.NotificationType;
import java.time.LocalDateTime;

public class Notification extends Entity {
  private int userId;
  private NotificationType type;
  private String content;
  private boolean read;
  private LocalDateTime createdAt;

  public Notification() {
    super();
  }

  public Notification(int id, int userId, NotificationType type, String content, boolean read) {
    super(id);
    this.userId = userId;
    this.type = type;
    this.content = content;
    this.read = read;
    this.createdAt = LocalDateTime.now();
  }

  public int getUserId() {
    return userId;
  }

  public NotificationType getType() {
    return type;
  }

  public String getContent() {
    return content;
  }

  public boolean isRead() {
    return read;
  }

  public void markRead() {
    this.read = true;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}

