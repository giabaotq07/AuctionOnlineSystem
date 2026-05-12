package app.service;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.data.BidResult;
import app.data.PlaceBidResponse;
import app.database.TransactionManager;
import app.models.Auction;

public class BidService {
  private final BidDAO bidDAO;
  private final AuctionDAO auctionDAO;
  private final TransactionManager transactionManager;
  private final BidValidator bidValidator;
  private final AntiSnipeService antiSnipeService;

  public BidService(BidDAO bidDAO, AuctionDAO auctionDAO) {
    this.bidDAO = bidDAO;
    this.auctionDAO = auctionDAO;
    this.transactionManager = new  TransactionManager();
    this.bidValidator = new BidValidator();
    this.antiSnipeService = new AntiSnipeService();
  }

  public PlaceBidResponse placeBid(int sessionId, int userId, long bidAmount) {
    BidResult result =
        transactionManager.runInTransaction(
            conn -> {
              auctionDAO.lockRow(conn, sessionId);
              Auction auction =
                  auctionDAO
                      .findById(conn, sessionId)
                      .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại."));
              bidValidator.validateAuctionState(auction);
              bidValidator.validateBidAmount(bidAmount, auction.getHighestBid());
              bidValidator.validateSelfBid(userId, auction.getWinnerId());
              auction.updateHighestBid(bidAmount, userId);
              antiSnipeService.apply(auction);
              bidDAO.insertBid(conn, sessionId, userId, bidAmount, false);
              auctionDAO.update(conn, auction);
              return new BidResult(auction.getId(), auction.getHighestBid(), auction.getWinnerId());
            });
    return new PlaceBidResponse(
        true, result.sessionId(), result.highestBid(), result.winnerId(), "Success");
  }
}
