package app.common.protocol;

import app.common.dto.Request;
import app.common.enums.RequestType;
import app.common.utils.JsonUtil;
import com.google.gson.Gson;

/** PacketReq. */
public class PacketReq {
  private static final Gson GSON = JsonUtil.gson();
  private RequestType type;
  private String data;

  /** PacketReq. */
  public PacketReq(RequestType type, String data) {
    this.type = type;
    this.data = data;
  }

  /** getData. */
  public <T extends Request> T getData(Class<T> clazz) {
    if (clazz == null || data == null) {
      return null;
    }
    return GSON.fromJson(data, clazz);
  }

  /** of. */
  public static PacketReq of(RequestType type, Request payload) {
    return new PacketReq(type, payload == null ? null : GSON.toJson(payload));
  }

  public RequestType getType() {
    return type;
  }
}
