package app.models;

import java.time.LocalDateTime;

public class HistoryRecord {

  private String sessionId;
  private HistoryType type;
  private String message;
  private LocalDateTime time;

  public HistoryRecord(String sessionId, HistoryType type, String message) {
    this.sessionId = sessionId;
    this.type = type;
    this.message = message;
    this.time = LocalDateTime.now();
  }

  public String getSessionId() {
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

  @Override
  public String toString() {
    return "[" + time + "] " + type + " - " + message;
  }
}
