package app.common.dto;

import java.time.LocalDateTime;
import java.util.List;

/** AuctionDetail. */
public record AuctionDetail(AuctionData auction, ItemData item, List<BidData> bidHistory) {
  public AuctionDetail(AuctionData auction, ItemData item) {
    this(auction, item, List.of());
  }

  public AuctionDetail {
    bidHistory = bidHistory == null ? List.of() : List.copyOf(bidHistory);
  }

  public int auctionId() {
    return auction.id();
  }

  public String itemName() {
    return item.name();
  }

  public String description() {
    return item.description();
  }

  public long startingPrice() {
    return item.startingPrice();
  }

  public long stepPrice() {
    return item.stepPrice();
  }

  public LocalDateTime startTime() {
    return auction.startTime();
  }

  public LocalDateTime endTime() {
    return auction.endTime();
  }

  public int version() {
    return auction.version();
  }
}
