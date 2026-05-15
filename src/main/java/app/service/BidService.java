package app.service;

import app.dao.AuctionDao;
import app.dao.BidDao;
import app.dao.UserDao;
import app.data.PlaceBidResponse;
import app.database.TransactionManager;
import app.exception.ServiceException;
import app.models.Auction;
import app.models.User;
import java.math.BigDecimal;

/** BidService. */
public class BidService {
  private final BidDao bidDao;
  private final AuctionDao auctionDao;
  private final UserDao userDao;
  private final TransactionManager transactionManager;
  private final BidValidator bidValidator;
  private final AntiSnipeService antiSnipeService;

  /** BidService. */
  public BidService(
      BidDao bidDao,
      AuctionDao auctionDao,
      UserDao userDao,
      TransactionManager transactionManager,
      BidValidator bidValidator,
      AntiSnipeService antiSnipeService) {
    this.bidDao = bidDao;
    this.auctionDao = auctionDao;
    this.userDao = userDao;
    this.transactionManager = transactionManager;
    this.bidValidator = bidValidator;
    this.antiSnipeService = antiSnipeService;
  }

  /** placeBid. */
  public PlaceBidResponse placeBid(int auctionId, int userId, long bidAmount) {
    return transactionManager.runInTransaction(
        conn -> {
          auctionDao.lockRow(conn, auctionId);
          Auction auction =
              auctionDao
                  .findById(conn, auctionId)
                  .orElseThrow(() -> new ServiceException("Phiên đấu giá không tồn tại."));
          if (auction.getSellerId() == userId) {
            throw new ServiceException("Người bán không được tự đặt giá sản phẩm của mình.");
          }
          bidValidator.validateAuctionState(auction);
          bidValidator.validateBidAmount(bidAmount, auction.getHighestBid());
          bidValidator.validateSelfBid(userId, auction.getWinnerId());
          userDao.lockRow(conn, userId);
          User bidder =
              userDao
                  .findById(conn, userId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
          try {
            bidder
                .getWallet()
                .setFrozenAmount(String.valueOf(auctionId), BigDecimal.valueOf(bidAmount));
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
          auction.updateHighestBid(bidAmount, userId);
          antiSnipeService.apply(auction);
          bidDao.insertBid(conn, auctionId, userId, bidAmount, false);
          userDao.update(conn, bidder);
          auctionDao.update(conn, auction);
          return new PlaceBidResponse(
              true, auction.getId(), auction.getHighestBid(), auction.getWinnerId(), "Success");
        });
  }
}
