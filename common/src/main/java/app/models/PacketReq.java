package app.models;

import app.enums.PacketType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** PacketReq. */
public class PacketReq {
  private static final Gson GSON = new GsonBuilder().create();
  private PacketType type;
  private String data;

  /** PacketReq. */
  public PacketReq(PacketType type, String data) {
    this.type = type;
    this.data = data;
  }

  public PacketType getType() {
    return type;
  }

  public String getRawData() {
    return data;
  }

  /** Member. */
  @SuppressWarnings("unchecked")
  public <T> T getData() {
    if (type == null || type.reqClass == null || type.reqClass == Void.class || data == null) {
      return null;
    }
    return (T) GSON.fromJson(data, type.reqClass);
  }

  /** getData. */
  public <T> T getData(Class<T> clazz) {
    if (clazz == null || data == null) {
      return null;
    }
    return GSON.fromJson(data, clazz);
  }

  /** of. */
  public static PacketReq of(PacketType type, Object payload) {
    return new PacketReq(type, payload == null ? null : GSON.toJson(payload));
  }

  /** of. */
  public static PacketReq of(PacketType type) {
    return new PacketReq(type, null);
  }
}
