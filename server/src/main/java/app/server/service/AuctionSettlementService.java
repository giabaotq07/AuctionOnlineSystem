package app.server.service;

import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.AutoBid;
import app.common.models.Bid;
import app.common.models.User;
import app.server.dao.AutoBidDAO;
import app.server.dao.BidDAO;
import app.server.dao.UserDAO;
import app.server.service.result.AuctionSettlementResult;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Handles wallet settlement/release for auction participants. */
public class AuctionSettlementService {
  private final BidDAO bidDAO;
  private final UserDAO userDAO;
  private final AutoBidDAO autoBidDAO;

  public AuctionSettlementService(BidDAO bidDAO, UserDAO userDAO) {
    this(bidDAO, userDAO, null);
  }

  /** AuctionSettlementService. */
  public AuctionSettlementService(BidDAO bidDAO, UserDAO userDAO, AutoBidDAO autoBidDAO) {
    this.bidDAO = bidDAO;
    this.userDAO = userDAO;
    this.autoBidDAO = autoBidDAO;
  }

  public Set<Integer> settleWallets(java.sql.Connection conn, Auction auction) {
    return settleWalletsWithResult(conn, auction).settledUserIds();
  }

  public AuctionSettlementResult settleWalletsWithResult(
      java.sql.Connection conn, Auction auction) {
    Set<Integer> bidderIds = bidderIds(conn, auction.getId());
    Set<Integer> settledUserIds = new LinkedHashSet<>();
    BigDecimal winningAmount = BigDecimal.ZERO;
    Integer winnerId = auction.getWinnerId();
    for (Integer bidderId : bidderIds) {
      userDAO.lockRow(conn, bidderId);
      User user =
          userDAO
              .findById(conn, bidderId)
              .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + bidderId));
      if (winnerId != null && winnerId.equals(bidderId)) {
        long amountDueValue =
            bidDAO
                .findHighestBid(conn, auction.getId())
                .filter(bid -> bid.getBidderId() == bidderId)
                .map(Bid::getAmount)
                .orElse(auction.getHighestBid());
        BigDecimal amountDue = BigDecimal.valueOf(amountDueValue);
        user.getWallet().setFrozenAmount(String.valueOf(auction.getId()), amountDue);
        winningAmount = user.getWallet().commitFrozen(String.valueOf(auction.getId()));
      } else {
        user.getWallet().releaseFrozen(String.valueOf(auction.getId()));
      }
      userDAO.update(conn, user);
      settledUserIds.add(bidderId);
    }
    if (winnerId != null && winningAmount.signum() > 0) {
      userDAO.lockRow(conn, auction.getSellerId());
      User seller =
          userDAO
              .findById(conn, auction.getSellerId())
              .orElseThrow(
                  () ->
                      new ServiceException("Không tìm thấy user với id: " + auction.getSellerId()));
      seller.getWallet().deposit(winningAmount);
      userDAO.update(conn, seller);
      settledUserIds.add(auction.getSellerId());
    }
    return new AuctionSettlementResult(winningAmount, settledUserIds);
  }

  public Set<Integer> releaseWallets(java.sql.Connection conn, Auction auction) {
    Set<Integer> bidderIds = bidderIds(conn, auction.getId());
    Set<Integer> releasedUserIds = new LinkedHashSet<>();
    for (Integer bidderId : bidderIds) {
      userDAO.lockRow(conn, bidderId);
      User user =
          userDAO
              .findById(conn, bidderId)
              .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + bidderId));
      user.getWallet().releaseFrozen(String.valueOf(auction.getId()));
      userDAO.update(conn, user);
      releasedUserIds.add(bidderId);
    }
    return releasedUserIds;
  }

  private Set<Integer> bidderIds(java.sql.Connection conn, int auctionId) {
    List<Bid> bids = bidDAO.findByAuction(conn, auctionId);
    Set<Integer> bidderIds = new LinkedHashSet<>();
    for (Bid bid : bids) {
      bidderIds.add(bid.getBidderId());
    }
    if (autoBidDAO != null) {
      for (AutoBid autoBid : autoBidDAO.findByAuction(conn, auctionId)) {
        bidderIds.add(autoBid.getUserId());
      }
    }
    return bidderIds;
  }
}
