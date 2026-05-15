package app.models;

import app.enums.PacketType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** PacketRes. */
public class PacketRes {
  private static final Gson GSON = new GsonBuilder().create();
  private final boolean success;
  private final PacketType type;
  private final String message;
  private final String data;

  /** PacketRes. */
  public PacketRes(boolean success, PacketType type, String message, String data) {
    this.success = success;
    this.type = type;
    this.message = message;
    this.data = data;
  }

  /** of. */
  public static PacketRes of(PacketType type, Object payload) {
    return new PacketRes(true, type, "OK", toJson(payload));
  }

  /** of. */
  public static PacketRes of(boolean success, PacketType type, Object payload) {
    return new PacketRes(success, type, success ? "OK" : "FAILED", toJson(payload));
  }

  /** of. */
  public static PacketRes of(boolean success, PacketType type, String message, Object payload) {
    return new PacketRes(success, type, message, toJson(payload));
  }

  /** success. */
  public static PacketRes success(PacketType type, String message) {
    return new PacketRes(true, type, message, null);
  }

  /** error. */
  public static PacketRes error(String message) {
    return new PacketRes(false, PacketType.ERROR, message, null);
  }

  /** error. */
  public static PacketRes error(PacketType type, String message) {
    return new PacketRes(false, type, message, null);
  }

  private static String toJson(Object payload) {
    if (payload == null) {
      return null;
    }
    return GSON.toJson(payload);
  }

  /** getData. */
  public <T> T getData(Class<T> clazz) {
    if (clazz == null || data == null || data.isBlank()) {
      return null;
    }
    return GSON.fromJson(data, clazz);
  }

  /** Member. */
  @SuppressWarnings("unchecked")
  public <T> T getData() {
    if (type == null
        || type.resClass == null
        || type.resClass == Void.class
        || data == null
        || data.isBlank()) {
      return null;
    }
    return (T) GSON.fromJson(data, type.resClass);
  }

  public boolean isSuccess() {
    return success;
  }

  public PacketType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public String getRawData() {
    return data;
  }

  @Override
  public String toString() {
    return GSON.toJson(this);
  }
}
