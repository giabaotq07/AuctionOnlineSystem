package app.common.dto;

import app.common.models.Auction;
import java.time.LocalDateTime;

/** AuctionDetail. */
public record AuctionDetail(Auction auction, ItemData item, long currentPrice) {
  public int auctionId() {
    return auction.getId();
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

  public LocalDateTime endTime() {
    return auction.getEndTime();
  }

  public int version() {
    return auction.getVersion();
  }
}
