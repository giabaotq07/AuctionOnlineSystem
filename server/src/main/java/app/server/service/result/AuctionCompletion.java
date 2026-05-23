package app.server.service.result;

import app.common.models.Bid;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

/** Auction completion result. */
public record AuctionCompletion(
    int auctionId,
    boolean completed,
    Optional<Bid> highestBid,
    BigDecimal winningAmount,
    Set<Integer> settledUserIds) {
  /** AuctionCompletion. */
  public AuctionCompletion {
    highestBid = highestBid == null ? Optional.empty() : highestBid;
    winningAmount = winningAmount == null ? BigDecimal.ZERO : winningAmount;
    settledUserIds = settledUserIds == null ? Set.of() : Set.copyOf(settledUserIds);
  }
}
