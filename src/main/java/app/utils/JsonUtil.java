package app.utils;

import app.exception.AppException;
import app.models.*;
import com.google.gson.*;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

public class JsonUtil {
  // Khởi tạo Adapter cho cấu trúc đa hình của User

  static RuntimeTypeAdapterFactory<User> userAdapterFactory =
      RuntimeTypeAdapterFactory.of(User.class, "role") // "role" là tên trường trong JSON
          .registerSubtype(Admin.class, "ADMIN")
          .registerSubtype(Seller.class, "SELLER")
          .registerSubtype(Bidder.class, "BIDDER");

  // GSON compact cho network protocol (single-line)
  private static final Gson GSON =
      new GsonBuilder().registerTypeAdapterFactory(userAdapterFactory).create();

  // GSON pretty-printed cho logging/debugging
  private static final Gson GSON_PRETTY =
      new GsonBuilder().registerTypeAdapterFactory(userAdapterFactory).setPrettyPrinting().create();

  // 1. Chuyển Object sang String (để gửi qua Socket) - COMPACT format
  public static String toJson(Object obj) {
    return GSON.toJson(obj);
  }

  // Utility: Convert Object to pretty-printed String (for debugging/logging)
  public static String toJsonPretty(Object obj) {
    return GSON_PRETTY.toJson(obj);
  }

  // 2. Chuyển Object sang JsonElement (để gán vào Packet.data)
  public static JsonElement toJsonElement(Object obj) {
    return GSON.toJsonTree(obj);
  }

  // 3. Parse từ String sang Object (Cách cũ bạn đang dùng)
  public static <T> T fromJson(String json, Class<T> clazz) {
    try {
      return GSON.fromJson(json, clazz);
    } catch (JsonSyntaxException e) {
      System.err.println("Lỗi parse JSON từ String: " + e.getMessage());
      throw new AppException("Dữ liệu không hợp lệ");
    }
  }

  // 4. Parse từ JsonElement sang Object (Dùng trong các Command)
  // Đây là hàm sẽ sửa lỗi "JsonElement is not compatible with String" trong ảnh của bạn
  public static <T> T fromJson(JsonElement json, Class<T> clazz) {
    try {
      return GSON.fromJson(json, clazz);
    } catch (JsonSyntaxException e) {
      System.err.println("Lỗi parse JSON từ JsonElement: " + e.getMessage());
      throw new AppException("Dữ liệu không hợp lệ");
    }
  }
}
