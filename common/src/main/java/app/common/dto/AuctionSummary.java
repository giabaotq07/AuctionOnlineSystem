package app.common.dto;

import app.common.enums.AuctionStatus;
import java.time.LocalDateTime;

/** AuctionSummary. */
public record AuctionSummary(
    int auctionId,
    int itemId,
    String itemName,
    String imageUrl,
    long currentPrice,
    LocalDateTime startTime,
    LocalDateTime endTime,
    AuctionStatus status,
    int version) {
  public AuctionSummary(
      int auctionId,
      int itemId,
      String itemName,
      String imageUrl,
      long currentPrice,
      LocalDateTime endTime,
      AuctionStatus status,
      int version) {
    this(auctionId, itemId, itemName, imageUrl, currentPrice, null, endTime, status, version);
  }
}
