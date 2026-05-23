package app.server.service.result;

import java.math.BigDecimal;
import java.util.Set;

/** Result of settling wallets for an auction. */
public record AuctionSettlementResult(BigDecimal winningAmount, Set<Integer> settledUserIds) {
  public AuctionSettlementResult {
    winningAmount = winningAmount == null ? BigDecimal.ZERO : winningAmount;
    settledUserIds = settledUserIds == null ? Set.of() : Set.copyOf(settledUserIds);
  }
}
