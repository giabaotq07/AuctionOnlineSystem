package app.utils;

import app.exception.AppException;
import com.google.gson.*;

public class JsonUtil {

  private static final Gson GSON = new GsonBuilder().create();

  public static String toJson(Object obj) {
    return GSON.toJson(obj);
  }

  public static <T> T fromJson(String json, Class<T> clazz) {
    try {
      return GSON.fromJson(json, clazz);
    } catch (JsonSyntaxException e) {
      System.err.println("Lỗi parse JSON từ String: " + e.getMessage());
      throw new AppException("Dữ liệu không hợp lệ");
    }
  }
}
