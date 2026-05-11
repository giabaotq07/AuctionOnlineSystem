package app.enums;

import app.data.AuctionDetailRequest;
import app.data.AuctionDetailResponse;
import app.data.AuctionResultRequest;
import app.data.AuctionResultResponse;
import app.data.AuctionsRequest;
import app.data.AuctionsResponse;
import app.data.ChatRequest;
import app.data.ChatResponse;
import app.data.CreateAuctionRequest;
import app.data.CreateAuctionResponse;
import app.data.HistoryRequest;
import app.data.HistoryResponse;
import app.data.LoginRequest;
import app.data.LoginResponse;
import app.data.PlaceBidRequest;
import app.data.PlaceBidResponse;
import app.data.RegisterRequest;
import app.data.RegisterResponse;

public enum PacketType {
  LOGIN(LoginRequest.class, LoginResponse.class),
  LOGOUT(Void.class, Void.class),
  REGISTER(RegisterRequest.class, RegisterResponse.class),
  PLACE_BID(PlaceBidRequest.class, PlaceBidResponse.class),
  CREATE_AUCTION(CreateAuctionRequest.class, CreateAuctionResponse.class),
  CHAT(ChatRequest.class, ChatResponse.class),
  FETCH_AUCTIONS(AuctionsRequest.class, AuctionsResponse.class),
  FETCH_HISTORY(HistoryRequest.class, HistoryResponse.class),
  FETCH_AUCTION_DETAIL(AuctionDetailRequest.class, AuctionDetailResponse.class),
  FETCH_AUCTION_RESULT(AuctionResultRequest.class, AuctionResultResponse.class),
  ERROR(Void.class, Void.class),
  SUCCESS(Void.class, Void.class);

  public final Class<?> reqClass;
  public final Class<?> resClass;

  PacketType(Class<?> reqClass, Class<?> resClass) {
    this.reqClass = reqClass;
    this.resClass = resClass;
  }
}
