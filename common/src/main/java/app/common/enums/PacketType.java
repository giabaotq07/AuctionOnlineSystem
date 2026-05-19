package app.common.enums;

import app.common.dto.AuctionDetailRequest;
import app.common.dto.AuctionDetailResponse;
import app.common.dto.AuctionHistoryResponse;
import app.common.dto.AuctionResultRequest;
import app.common.dto.AuctionResultResponse;
import app.common.dto.AuctionSummariesResponse;
import app.common.dto.CancelAuctionRequest;
import app.common.dto.CancelAuctionResponse;
import app.common.dto.ChatRequest;
import app.common.dto.ChatResponse;
import app.common.dto.CreateAuctionRequest;
import app.common.dto.CreateAuctionResponse;
import app.common.dto.DeleteItemRequest;
import app.common.dto.DepositRequest;
import app.common.dto.FetchSellerItemsRequest;
import app.common.dto.ItemListResponse;
import app.common.dto.ItemResponse;
import app.common.dto.LoginRequest;
import app.common.dto.LoginResponse;
import app.common.dto.PlaceBidRequest;
import app.common.dto.PlaceBidResponse;
import app.common.dto.RegisterRequest;
import app.common.dto.RegisterResponse;
import app.common.dto.SettleWalletRequest;
import app.common.dto.UpdateItemRequest;
import app.common.dto.UserListResponse;
import app.common.dto.WalletUpdateResponse;

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
