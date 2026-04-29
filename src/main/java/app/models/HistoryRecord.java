package app.models;

import java.time.LocalDateTime;

public class HistoryRecord implements java.io.Serializable {
  private int sessionId;
  private HistoryType type;
  private String message;
  private LocalDateTime time;

  public HistoryRecord(int sessionId, HistoryType type, String message) {
    this.sessionId = sessionId;
    this.type = type;
    this.message = message;
    this.time = LocalDateTime.now();
  }

  public HistoryRecord(int sessionId, HistoryType type, String message, LocalDateTime time) {
    this.sessionId = sessionId;
    this.type = type;
    this.message = message;
    this.time = time;
  }

  public int getSessionId() {
    return sessionId;
  }

  public HistoryType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public LocalDateTime getTime() {
    return time;
  }
}
