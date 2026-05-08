package app.models;

import app.enums.HistoryType;
import java.io.Serializable;
import java.time.LocalDateTime;

public class HistoryRecord implements Serializable {
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

  public int getSessionId() {
    return sessionId;
  }

  public void setSessionId(int sessionId) {
    this.sessionId = sessionId;
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
