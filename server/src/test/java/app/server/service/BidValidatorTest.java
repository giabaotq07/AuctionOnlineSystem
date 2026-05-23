package app.server.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.common.enums.AuctionStatus;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BidValidatorTest {
  private final BidValidator validator = new BidValidator();

  @Test
  void validateAuctionStateRejectsInvalidStates() {
    assertThrows(ServiceException.class, () -> validator.validateAuctionState(null));

    Auction openAuction = new Auction(1, 1, LocalDateTime.now().plusMinutes(30), 100L);
    assertThrows(ServiceException.class, () -> validator.validateAuctionState(openAuction));

    openAuction.setStatus(AuctionStatus.FINISHED);
    assertThrows(ServiceException.class, () -> validator.validateAuctionState(openAuction));
  }

  @Test
  void validateAuctionStateRejectsExpiredRunningAuction() {
    Auction auction = new Auction(1, 1, LocalDateTime.now().minusMinutes(1), 100L);
    auction.start();

    assertThrows(ServiceException.class, () -> validator.validateAuctionState(auction));
  }

  @Test
  void validateAuctionStateAllowsRunningAuction() {
    Auction auction = new Auction(1, 1, LocalDateTime.now().plusMinutes(30), 100L);
    auction.start();

    assertDoesNotThrow(() -> validator.validateAuctionState(auction));
  }

  @Test
  void validateBidAmountRequiresPositiveStepAndMinimumBid() {
    assertThrows(ServiceException.class, () -> validator.validateBidAmount(120L, 100L, 0L));
    assertThrows(ServiceException.class, () -> validator.validateBidAmount(0L, 100L, 10L));
    assertThrows(ServiceException.class, () -> validator.validateBidAmount(109L, 100L, 10L));

    assertDoesNotThrow(() -> validator.validateBidAmount(110L, 100L, 10L));
  }
}
