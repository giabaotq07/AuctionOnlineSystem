package app.utils;

import app.data.AuctionDetailResponse;
import app.data.AuctionResultResponse;
import app.data.AuctionsResponse;
import app.data.ChatResponse;
import app.data.CreateAuctionResponse;
import app.data.HistoryResponse;
import app.data.LoginResponse;
import app.data.PlaceBidResponse;
import app.data.RegisterResponse;
import app.data.Response;
import app.enums.PacketType;
import app.exception.AppException;
import app.models.*;
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

  public static Response decodeResponse(Packet packet) {
    if (packet == null || packet.getData() == null) {
      return null;
    }
    PacketType type = packet.getType();
    if (type == null) {
      return null;
    }
    switch (type) {
      case LOGIN:
        return JsonUtil.fromJson(packet.getData(), LoginResponse.class);
      case REGISTER:
        return JsonUtil.fromJson(packet.getData(), RegisterResponse.class);
      case PLACE_BID:
        return JsonUtil.fromJson(packet.getData(), PlaceBidResponse.class);
      case CREATE_AUCTION:
        return JsonUtil.fromJson(packet.getData(), CreateAuctionResponse.class);
      case CHAT:
        return JsonUtil.fromJson(packet.getData(), ChatResponse.class);
      case FETCH_AUCTIONS:
        return JsonUtil.fromJson(packet.getData(), AuctionsResponse.class);
      case FETCH_HISTORY:
        return JsonUtil.fromJson(packet.getData(), HistoryResponse.class);
      case FETCH_AUCTION_DETAIL:
        return JsonUtil.fromJson(packet.getData(), AuctionDetailResponse.class);
      case FETCH_AUCTION_RESULT:
        return JsonUtil.fromJson(packet.getData(), AuctionResultResponse.class);
      default:
        return null;
    }
  }
}
