package app.common.protocol;

import app.common.enums.ResponseType;
import app.common.utils.JsonUtil;
import com.google.gson.Gson;

/** PacketRes. */
public class PacketRes {
  private static final Gson GSON = JsonUtil.gson();
  private final boolean success;
  private final ResponseType type;
  private final String message;
  private final String data;

  /** PacketRes. */
  public PacketRes(boolean success, ResponseType type, String message, String data) {
    this.success = success;
    this.type = type;
    this.message = message;
    this.data = data;
  }

  public static PacketRes of(ResponseType type, String message, Object payload) {
    return new PacketRes(true, type, message, toJson(payload));
  }

  /** error. */
  public static PacketRes error(ResponseType type, String message) {
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
    if (clazz == null || data == null) {
      return null;
    }
    return GSON.fromJson(data, clazz);
  }

  public boolean isSuccess() {
    return success;
  }

  public ResponseType getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return GSON.toJson(this);
  }
}
