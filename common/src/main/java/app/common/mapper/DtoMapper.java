package app.common.mapper;

import app.common.dto.*;
import app.common.models.*;
import java.util.Optional;

/** DtoMapper. */
public final class DtoMapper {
  private DtoMapper() {}

  /** toUserData. */
  public static UserData toUserData(User user) {
    return new UserData(
        user.getId(),
        user.getName(),
        user.getAccount().getUsername(),
        user.getWallet().getAvailableBalance(),
        user.getWallet().getFrozenFundsSnapshot(),
        user.getRole());
  }

  /** toItemData. */
  public static ItemData toItemData(Item item) {
    return new ItemData(
        item.getId(),
        item.getSellerId(),
        item.getName(),
        item.getDescription(),
        item.getStartingPrice(),
        item.getStepPrice(),
        item.getType(),
        item.isDeleted());
  }

  /** toAuctionSummary. */
  public static AuctionSummary toAuctionSummary(Auction auction, Item item) {
    return new AuctionSummary(
        auction.getId(),
        item.getName(),
        currentPrice(auction, item),
        auction.getEndTime(),
        auction.getStatus(),
        auction.getVersion());
  }

  /** toAuctionDetail. */
  public static AuctionDetail toAuctionDetail(Auction auction, Item item) {
    return new AuctionDetail(auction, toItemData(item), currentPrice(auction, item));
  }

  /** toAuctionResultResponse. */
  public static AuctionResultResponse toAuctionResultResponse(
      int auctionId, Optional<BidTransaction> highestBid) {
    if (highestBid.isEmpty()) {
      return new AuctionResultResponse(auctionId, new ProfileData(0, "chưa có người thắng"), 0);
    }
    BidTransaction bid = highestBid.get();
    return new AuctionResultResponse(
        auctionId, new ProfileData(bid.getBidderId(), bid.getBidderName()), bid.getAmount());
  }

  /** toUser. */
  public static User toUser(UserData userData) {
    Wallet wallet = new Wallet(userData.availableBalance(), userData.frozenFunds());
    return UserFactory.createUser(
        userData.id(),
        userData.name(),
        new Account(userData.username(), null),
        wallet,
        userData.role());
  }

  private static long currentPrice(Auction auction, Item item) {
    return auction.getHighestBid() > 0 ? auction.getHighestBid() : item.getStartingPrice();
  }
}
