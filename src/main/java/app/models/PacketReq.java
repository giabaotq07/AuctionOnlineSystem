package app.models;

import app.enums.PacketType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PacketReq {
  private static final Gson GSON = new GsonBuilder().create();

  private PacketType type;
  private String data;

  public PacketReq(PacketType type, String data) {
    this.type = type;
    this.data = data;
  }

  // Return the packet type.
  public PacketType getType() {
    return type;
  }

  // Return the raw JSON payload.
  public String getRawData() {
    return data;
  }

  // Deserialize payload based on the mapped request class.
  @SuppressWarnings("unchecked")
  public <T> T getData() {
    if (type == null || type.reqClass == null || type.reqClass == Void.class || data == null) {
      return null;
    }
    return (T) GSON.fromJson(data, type.reqClass);
  }

  // Deserialize payload using the provided class.
  public <T> T getData(Class<T> clazz) {
    if (clazz == null || data == null) {
      return null;
    }
    return GSON.fromJson(data, clazz);
  }

  // Create a request packet from a payload object.
  public static PacketReq of(PacketType type, Object payload) {
    return new PacketReq(type, GSON.toJson(payload));
  }
}
