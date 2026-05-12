package app.service;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.data.PlaceBidResponse;
import app.database.TransactionManager;
import app.exception.ServiceException;
import app.models.Auction;

public class BidService {
  private final BidDAO bidDAO;
  private final AuctionDAO auctionDAO;
  private final TransactionManager transactionManager;
  private final BidValidator bidValidator;
  private final AntiSnipeService antiSnipeService;

  // Updated constructor to receive dependencies via injection for testability
  public BidService(
      BidDAO bidDAO,
      AuctionDAO auctionDAO,
      TransactionManager transactionManager,
      BidValidator bidValidator,
      AntiSnipeService antiSnipeService) {
    this.bidDAO = bidDAO;
    this.auctionDAO = auctionDAO;
    this.transactionManager = transactionManager;
    this.bidValidator = bidValidator;
    this.antiSnipeService = antiSnipeService;
  }

  public PlaceBidResponse placeBid(int auctionId, int userId, long bidAmount) {
    return transactionManager.runInTransaction(
        conn -> {
          auctionDAO.lockRow(conn, auctionId);
          Auction auction =
              auctionDAO
                  .findById(conn, auctionId)
                  .orElseThrow(() -> new ServiceException("Phiên đấu giá không tồn tại."));
          bidValidator.validateAuctionState(auction);
          bidValidator.validateBidAmount(bidAmount, auction.getHighestBid());
          bidValidator.validateSelfBid(userId, auction.getWinnerId());
          auction.updateHighestBid(bidAmount, userId);
          antiSnipeService.apply(auction);
          bidDAO.insertBid(conn, auctionId, userId, bidAmount, false);
          auctionDAO.update(conn, auction);
          return new PlaceBidResponse(
              true, auction.getId(), auction.getHighestBid(), auction.getWinnerId(), "Success");
        });
  }
}
