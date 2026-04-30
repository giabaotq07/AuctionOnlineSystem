package app.models;

import app.enums.CommandType;
import java.io.Serializable;

public class MessagePacket<T> implements Serializable {
  private CommandType type;
  private String message;
  private T data;

  public MessagePacket(CommandType type, T data) {
    this.type = type;
    this.data = data;
  }

  public static <T> MessagePacket<T> error(String msg) {
    MessagePacket<T> packet = new MessagePacket<>(CommandType.ERROR, null);
    packet.message = msg;
    return packet;
  }

  public void setType(CommandType type) {
    this.type = type;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setData(T data) {
    this.data = data;
  }

  public CommandType getType() {
    return type;
  }

  public T getData() {
    return data;
  }

  public String getMessage() {
    return message;
  }
}
