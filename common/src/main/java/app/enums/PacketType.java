package app.enums;

import app.dto.AuctionDetailRequest;
import app.dto.AuctionDetailResponse;
import app.dto.AuctionHistoryResponse;
import app.dto.AuctionResultRequest;
import app.dto.AuctionResultResponse;
import app.dto.AuctionSummariesResponse;
import app.dto.CancelAuctionRequest;
import app.dto.CancelAuctionResponse;
import app.dto.ChatRequest;
import app.dto.ChatResponse;
import app.dto.CreateAuctionRequest;
import app.dto.CreateAuctionResponse;
import app.dto.DeleteItemRequest;
import app.dto.DepositRequest;
import app.dto.FetchSellerItemsRequest;
import app.dto.ItemListResponse;
import app.dto.ItemResponse;
import app.dto.LoginRequest;
import app.dto.LoginResponse;
import app.dto.PlaceBidRequest;
import app.dto.PlaceBidResponse;
import app.dto.RegisterRequest;
import app.dto.RegisterResponse;
import app.dto.SettleWalletRequest;
import app.dto.UpdateItemRequest;
import app.dto.UserListResponse;
import app.dto.WalletUpdateResponse;

/** PacketType. */
public enum PacketType {
  LOGIN(LoginRequest.class, LoginResponse.class),
  LOGOUT(Void.class, Void.class),
  REGISTER(RegisterRequest.class, RegisterResponse.class),
  PLACE_BID(PlaceBidRequest.class, PlaceBidResponse.class),
  CREATE_AUCTION(CreateAuctionRequest.class, CreateAuctionResponse.class),
  CHAT(ChatRequest.class, ChatResponse.class),
  FETCH_AUCTION_SUMMARIES(Void.class, AuctionSummariesResponse.class),
  FETCH_AUCTION_HISTORY(Void.class, AuctionHistoryResponse.class),
  FETCH_AUCTION_DETAIL(AuctionDetailRequest.class, AuctionDetailResponse.class),
  FETCH_AUCTION_RESULT(AuctionResultRequest.class, AuctionResultResponse.class),
  FETCH_SELLER_ITEMS(FetchSellerItemsRequest.class, ItemListResponse.class),
  UPDATE_ITEM(UpdateItemRequest.class, ItemResponse.class),
  DELETE_ITEM(DeleteItemRequest.class, ItemResponse.class),
  FETCH_USER_LIST(Void.class, UserListResponse.class),
  CANCEL_AUCTION(CancelAuctionRequest.class, CancelAuctionResponse.class),
  DEPOSIT(DepositRequest.class, WalletUpdateResponse.class),
  SETTLE_WALLET(SettleWalletRequest.class, WalletUpdateResponse.class),
  WALLET_UPDATE(Void.class, WalletUpdateResponse.class),
  ERROR(Void.class, Void.class);
  public final Class<?> reqClass;
  public final Class<?> resClass;

  PacketType(Class<?> reqClass, Class<?> resClass) {
    this.reqClass = reqClass;
    this.resClass = resClass;
  }
}
