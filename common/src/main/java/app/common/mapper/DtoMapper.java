package app.common.mapper;

import app.common.dto.*;
import app.common.models.*;
import java.util.List;
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
        item.getImageUrl(),
        item.getStartingPrice(),
        item.getStepPrice(),
        item.getType(),
        item.isDeleted());
  }

  /** toAuctionData. */
  public static AuctionData toAuctionData(Auction auction) {
    return new AuctionData(
        auction.getId(),
        auction.getItemId(),
        auction.getSellerId(),
        auction.getWinnerId(),
        auction.getStatus(),
        auction.getStartTime(),
        auction.getEndTime(),
        auction.getHighestBid(),
        auction.getExtendedCount(),
        auction.getVersion(),
        auction.getCreatedAt(),
        auction.getUpdatedAt());
  }

  /** toAuctionSummary. */
  public static AuctionSummary toAuctionSummary(Auction auction, Item item) {
    return new AuctionSummary(
        auction.getId(),
        item == null ? auction.getItemId() : item.getId(),
        item == null ? auction.getItemName() : item.getName(),
        item == null ? auction.getImageUrl() : item.getImageUrl(),
        currentPrice(auction, item),
        auction.getStartTime(),
        auction.getEndTime(),
        auction.getStatus(),
        auction.getVersion());
  }

  /** toAuctionDetail. */
  public static AuctionDetail toAuctionDetail(Auction auction, Item item) {
    return new AuctionDetail(toAuctionData(auction), toItemData(item));
  }

  /** toAuctionDetail. */
  public static AuctionDetail toAuctionDetail(Auction auction, Item item, List<Bid> bids) {
    List<BidData> bidHistory =
        bids == null ? List.of() : bids.stream().map(DtoMapper::toBidData).toList();
    return new AuctionDetail(toAuctionData(auction), toItemData(item), bidHistory);
  }

  /** toBidData. */
  public static BidData toBidData(Bid bid) {
    return new BidData(
        bid.getId(),
        bid.getAuctionId(),
        bid.getBidderId(),
        bid.getBidderName(),
        bid.getAmount(),
        bid.getCreateAt(),
        bid.isAutoBid());
  }

  /** toItem. */
  public static Item toItem(ItemData itemData) {
    if (itemData == null) {
      return null;
    }
    Item item =
        ItemFactory.createItem(
            itemData.id(),
            itemData.name(),
            itemData.sellerId(),
            itemData.description(),
            itemData.startingPrice(),
            itemData.stepPrice(),
            itemData.type());
    item.setDeleted(itemData.deleted());
    item.setImageUrl(itemData.imageUrl());
    return item;
  }

  /** toAuction. */
  public static Auction toAuction(AuctionData auctionData) {
    if (auctionData == null) {
      return null;
    }
    return new Auction(
        auctionData.id(),
        auctionData.itemId(),
        auctionData.sellerId(),
        auctionData.winnerId(),
        auctionData.status(),
        auctionData.startTime(),
        auctionData.endTime(),
        auctionData.highestBid(),
        auctionData.extendedCount(),
        auctionData.version(),
        auctionData.createdAt(),
        auctionData.updatedAt());
  }

  /** toAuction. */
  public static Auction toAuction(AuctionSummary summary) {
    if (summary == null) {
      return null;
    }
    Auction auction =
        new Auction(
            summary.auctionId(),
            summary.itemId(),
            0,
            null,
            summary.status(),
            summary.startTime(),
            summary.endTime(),
            summary.currentPrice(),
            0,
            summary.version(),
            null,
            null);
    auction.setItemName(summary.itemName());
    auction.setImageUrl(summary.imageUrl());
    return auction;
  }

  /** toAuctionResultResponse. */
  public static AuctionResultResponse toAuctionResultResponse(
      int auctionId, Optional<Bid> highestBid) {
    if (highestBid.isEmpty()) {
      return new AuctionResultResponse(auctionId, new ProfileData(0, "chưa có người thắng"), 0);
    }
    Bid bid = highestBid.get();
    return new AuctionResultResponse(
        auctionId, new ProfileData(bid.getBidderId(), bid.getBidderName()), bid.getAmount());
  }

  /** toUser. */
  public static User toUser(UserData userData) {
    Wallet wallet = new Wallet(userData.availableBalance(), userData.frozenFunds());
    return new User(
        userData.id(),
        userData.name(),
        new Account(userData.username(), null, userData.role()),
        wallet);
  }

  private static long currentPrice(Auction auction, Item item) {
    if (auction.getHighestBid() > 0 || item == null) {
      return auction.getHighestBid();
    }
    return item.getStartingPrice();
  }
}
