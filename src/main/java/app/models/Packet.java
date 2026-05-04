package app.models;

import app.enums.PacketType;
import java.io.Serializable;

public class Packet implements Serializable {
  private PacketType type;
  private Object data;

  public Packet(PacketType type, Object data) {
    this.type = type;
    this.data = data;
  }

  public void setType(PacketType type) {
    this.type = type;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public PacketType getType() {
    return type;
  }

  public Object getData() {
    return data;
  }
}
