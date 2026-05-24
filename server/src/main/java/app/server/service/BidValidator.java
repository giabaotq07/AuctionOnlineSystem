package app.server.service;

import app.common.enums.AuctionStatus;
import app.common.exception.ServiceException;
import app.common.models.Auction;

/** BidValidator. */
public class BidValidator {
  /** validateAuctionState. */
  public void validateAuctionState(Auction auction) {
    if (auction == null) {
      throw new ServiceException("Phiên đấu giá không tồn tại.");
    }
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      throw new ServiceException("Phiên đấu giá chưa mở hoặc đã đóng.");
    }
    if (auction.isExpired()) {
      throw new ServiceException("Phiên đấu giá đã kết thúc.");
    }
  }

  /** validateBidAmount. */
  public void validateBidAmount(long bidAmount, long currentPrice, long stepPrice) {
    if (stepPrice <= 0) {
      throw new ServiceException("Bước giá không hợp lệ.");
    }
    if (bidAmount <= 0) {
      throw new ServiceException("Giá đặt không hợp lệ.");
    }
    long minimumBid = currentPrice + stepPrice;
    if (bidAmount < minimumBid) {
      throw new ServiceException("Giá đặt phải từ " + minimumBid + " trở lên.");
    }
  }
}
