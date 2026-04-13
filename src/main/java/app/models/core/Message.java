package app.models.core;

import java.io.Serializable;

public record Message(String clientIp, String message) implements Serializable {

  @Override
  public String toString() {
    return "[" + clientIp + "]: " + message;
  }
}
