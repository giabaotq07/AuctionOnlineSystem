package app.models;

import app.enums.PacketType;

public class Packet {
  private PacketType type;
  private String data;

  public Packet(PacketType type, String data) {
    this.type = type;
    this.data = data;
  }

  public void setType(PacketType type) {
    this.type = type;
  }

  public void setData(String data) {
    this.data = data;
  }

  public PacketType getType() {
    return type;
  }

  public String getData() {
    return data;
  }

  @Override
  public String toString() {
    return "Packet{" + "type=" + type + ", data=" + data + '}';
  }
}
