package app.service;

import app.enums.AuctionStatus;
import app.exception.ServiceException;
import app.models.Auction;

public class BidValidator {
  private static final long MIN_INCREMENT = 1L;

  public void validateAuctionState(Auction auction) {
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      throw new ServiceException(
          "Phiên đấu giá đã " + auction.getStatus().name().toLowerCase() + ".");
    }
  }

  public void validateBidAmount(long bidAmount, long currentPrice) {
    long minimumRequired = currentPrice + MIN_INCREMENT;
    if (bidAmount < minimumRequired) {
      throw new ServiceException("Giá đặt phải tối thiểu " + minimumRequired + " VNĐ.");
    }
  }

  public void validateSelfBid(int userId, Integer currentWinnerId) {
    if (currentWinnerId != null && currentWinnerId == userId) {
      throw new ServiceException("Bạn đang là người giữ giá cao nhất.");
    }
  }
}
