package app.server.service;

import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.AuctionDAO;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.database.TransactionManager;
import java.math.BigDecimal;

/** BidService. */
public class BidService {
  private final BidDAO bidDAO;
  private final AuctionDAO auctionDAO;
  private final ItemDAO itemDAO;
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;
  private final BidValidator bidValidator;
  private final AntiSnipeService antiSnipeService;
  private final AutoBidService autoBidService;

  /** BidService. */
  public BidService(
      BidDAO bidDAO,
      AuctionDAO auctionDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      TransactionManager transactionManager,
      BidValidator bidValidator,
      AntiSnipeService antiSnipeService,
      AutoBidService autoBidService) {
    this.bidDAO = bidDAO;
    this.auctionDAO = auctionDAO;
    this.itemDAO = itemDAO;
    this.userDAO = userDAO;
    this.transactionManager = transactionManager;
    this.bidValidator = bidValidator;
    this.antiSnipeService = antiSnipeService;
    this.autoBidService = autoBidService;
  }

  public Auction placeBid(int auctionId, User actor, long bidAmount) {

    return transactionManager.runInTransaction(
        conn -> {
          int bidderId = actor.getId();
          auctionDAO.lockRow(conn, auctionId);
          Auction auction =
              auctionDAO
                  .findById(conn, auctionId)
                  .orElseThrow(() -> new ServiceException("Phiên đấu giá không tồn tại."));

          bidValidator.validateAuctionState(auction);
          Item item =
              itemDAO
                  .findById(conn, auction.getItemId())
                  .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
          bidValidator.validateBidAmount(bidAmount, auction.getHighestBid(), item.getStepPrice());
          userDAO.lockRow(conn, bidderId);
          User bidder =
              userDAO
                  .findById(conn, bidderId)
                  .orElseThrow(
                      () -> new ServiceException("Không tìm thấy user với id: " + bidderId));
          try {
            long reserveAmount =
                autoBidService.reserveAmountForManualBid(conn, auctionId, bidderId, bidAmount);
            bidder
                .getWallet()
                .setFrozenAmount(String.valueOf(auctionId), BigDecimal.valueOf(reserveAmount));
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
          auction.updateHighestBid(bidAmount, bidderId);
          bidDAO.insertBid(conn, auctionId, bidderId, bidAmount, false);
          userDAO.update(conn, bidder);
          autoBidService.resolveAutoBid(conn, auction, item);
          antiSnipeService.apply(auction);
          auctionDAO.update(conn, auction);
          return auction;
        });
  }
}
