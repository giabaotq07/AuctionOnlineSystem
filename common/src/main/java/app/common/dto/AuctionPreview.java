package app.common.dto;

import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.models.Auction;
import app.common.models.Item;
import java.time.LocalDateTime;

/** Lightweight auction projection for list and history screens. */
public record AuctionPreview(
    int auctionId,
    int itemId,
    String itemName,
    String imageUrl,
    ItemType itemType,
    AuctionStatus status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    long highestBid,
    long startingPrice,
    long stepPrice,
    int version,
    UserPreview seller) {
  public static AuctionPreview from(Auction auction) {
    if (auction == null) {
      return null;
    }
    Item item = auction.getItem();
    return new AuctionPreview(
        auction.getId(),
        item == null ? auction.getItemId() : item.getId(),
        item == null ? null : item.getName(),
        item == null ? null : item.getImageUrl(),
        item == null ? null : item.getType(),
        auction.getStatus(),
        auction.getStartTime(),
        auction.getEndTime(),
        auction.getHighestBid(),
        item == null ? 0 : item.getStartingPrice(),
        item == null ? 1 : item.getStepPrice(),
        auction.getVersion(),
        UserPreview.from(auction.getSeller()));
  }

  public AuctionPreview withHighestBid(long nextHighestBid) {
    return new AuctionPreview(
        auctionId,
        itemId,
        itemName,
        imageUrl,
        itemType,
        status,
        startTime,
        endTime,
        nextHighestBid,
        startingPrice,
        stepPrice,
        version,
        seller);
  }

  public AuctionPreview withStatus(AuctionStatus nextStatus) {
    return new AuctionPreview(
        auctionId,
        itemId,
        itemName,
        imageUrl,
        itemType,
        nextStatus,
        startTime,
        endTime,
        highestBid,
        startingPrice,
        stepPrice,
        version,
        seller);
  }

  public AuctionPreview withStatusAndHighestBid(AuctionStatus nextStatus, long nextHighestBid) {
    return new AuctionPreview(
        auctionId,
        itemId,
        itemName,
        imageUrl,
        itemType,
        nextStatus,
        startTime,
        endTime,
        nextHighestBid,
        startingPrice,
        stepPrice,
        version,
        seller);
  }
}
