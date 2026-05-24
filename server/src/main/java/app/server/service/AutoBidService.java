package app.server.service;

import app.common.dto.WalletUpdateResponse;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.AutoBid;
import app.common.models.Bid;
import app.common.models.Item;
import app.common.models.User;
import app.common.protocol.PacketRes;
import app.server.dao.AuctionDAO;
import app.server.dao.AutoBidDAO;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.database.TransactionManager;
import app.server.service.result.AutoBidUpdateResult;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Comparator;
import java.util.List;

/** Server-side proxy bidding operations. */
public class AutoBidService {
  private final AutoBidDAO autoBidDAO;
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final ItemDAO itemDAO;
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;
  private final BidValidator bidValidator;

  /** AutoBidService. */
  public AutoBidService(
      AutoBidDAO autoBidDAO,
      AuctionDAO auctionDAO,
      BidDAO bidDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      TransactionManager transactionManager,
      BidValidator bidValidator) {
    this.autoBidDAO = autoBidDAO;
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
    this.itemDAO = itemDAO;
    this.userDAO = userDAO;
    this.transactionManager = transactionManager;
    this.bidValidator = bidValidator;
  }

  /** getAutoBid. */
  public java.util.Optional<AutoBid> getAutoBid(int auctionId, int userId) {
    return autoBidDAO.findByAuctionAndUser(auctionId, userId);
  }

  /** setAutoBid. */
  public AutoBidUpdateResult setAutoBid(
      int auctionId, User actor, long maxAmount, long incrementAmount) {
    validateAutoBidActor(auctionId, actor);

    return transactionManager.runInTransaction(
        conn -> {
          Auction auction = lockedRunningAuction(conn, auctionId);
          Item item = auctionItem(conn, auction);
          validateAutoBidRules(auction, item, actor, maxAmount, incrementAmount);

          User bidder = lockedUser(conn, actor.getId());
          try {
            bidder
                .getWallet()
                .setFrozenAmount(String.valueOf(auctionId), BigDecimal.valueOf(maxAmount));
          } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
          }
          userDAO.update(conn, bidder);

          AutoBid autoBid =
              autoBidDAO
                  .findByAuctionAndUser(conn, auctionId, actor.getId())
                  .map(
                      existing -> {
                        autoBidDAO.delete(conn, existing.getId());
                        return saveAutoBid(
                            conn, auctionId, actor.getId(), maxAmount, incrementAmount);
                      })
                  .orElseGet(
                      () ->
                          saveAutoBid(conn, auctionId, actor.getId(), maxAmount, incrementAmount));
          AutoBid currentAutoBid =
              autoBidDAO.findByAuctionAndUser(conn, auctionId, actor.getId()).orElse(autoBid);
          User currentBidder = userDAO.findById(conn, actor.getId()).orElse(bidder);
          return new AutoBidUpdateResult(auction, currentAutoBid, currentBidder);
        });
  }

  /** disableAutoBid. */
  public AutoBidUpdateResult disableAutoBid(int auctionId, User actor) {
    validateAutoBidActor(auctionId, actor);

    return transactionManager.runInTransaction(
        conn -> {
          Auction auction = lockedRunningAuction(conn, auctionId);

          AutoBid autoBid =
              autoBidDAO
                  .findByAuctionAndUser(conn, auctionId, actor.getId())
                  .orElseThrow(() -> new ServiceException("Không tìm thấy auto-bid để tắt."));
          autoBid.setEnabled(false);
          autoBidDAO.update(conn, autoBid);

          User bidder = lockedUser(conn, actor.getId());
          long retainedBid = highestBidForUser(conn, auctionId, actor.getId());
          bidder
              .getWallet()
              .setFrozenAmount(String.valueOf(auctionId), BigDecimal.valueOf(retainedBid));
          userDAO.update(conn, bidder);
          return new AutoBidUpdateResult(auction, autoBid, bidder);
        });
  }

  /** Keeps an existing enabled auto-bid reservation from being reduced by a manual bid. */
  public long reserveAmountForManualBid(
      Connection conn, int auctionId, int bidderId, long manualBidAmount) {
    return autoBidDAO
        .findByAuctionAndUser(conn, auctionId, bidderId)
        .filter(AutoBid::isEnabled)
        .map(autoBid -> Math.max(manualBidAmount, autoBid.getMaxAmount()))
        .orElse(manualBidAmount);
  }

  /** resolveAutoBid. */
  public boolean resolveAutoBid(Connection conn, Auction auction, Item item) {
    List<AutoBid> autoBids = autoBidDAO.findEnabledByAuction(conn, auction.getId());
    if (autoBids.isEmpty()) {
      return false;
    }

    autoBids.sort(
        Comparator.comparingLong(AutoBid::getMaxAmount)
            .reversed()
            .thenComparing(AutoBid::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingInt(AutoBid::getId));

    AutoBid winner = autoBids.get(0);
    boolean hasRunner =
        auction.getWinnerId() == null || auction.getWinnerId() != winner.getUserId();
    long runnerMax = hasRunner ? auction.getHighestBid() : 0;
    for (int i = 1; i < autoBids.size(); i++) {
      AutoBid runner = autoBids.get(i);
      if (runner.getUserId() != winner.getUserId()) {
        runnerMax = Math.max(runnerMax, runner.getMaxAmount());
        hasRunner = true;
        break;
      }
    }
    if (!hasRunner) {
      return false;
    }

    long increment = Math.max(item.getStepPrice(), winner.getIncrementAmount());
    long finalBid = Math.min(winner.getMaxAmount(), runnerMax + increment);
    if (finalBid < auction.getHighestBid()) {
      return false;
    }

    Integer previousWinner = auction.getWinnerId();
    boolean changed = false;
    if (finalBid > auction.getHighestBid()) {
      auction.updateHighestBid(finalBid, winner.getUserId());
      changed = true;
    } else if (previousWinner == null || previousWinner != winner.getUserId()) {
      auction.setWinnerId(winner.getUserId());
      changed = true;
    }
    if (changed) {
      bidDAO.insertBid(conn, auction.getId(), winner.getUserId(), finalBid, true);
    }

    // Check for outbid auto-bids
    long newHighest = auction.getHighestBid();
    for (AutoBid ab : autoBids) {
      if (ab.isEnabled() && ab.getMaxAmount() < newHighest) {
        ab.setEnabled(false);
        autoBidDAO.update(conn, ab);
        try {
          userDAO.lockRow(conn, ab.getUserId());
          User outbidUser = userDAO.findById(conn, ab.getUserId()).orElse(null);
          if (outbidUser != null) {
            long retainedBid = highestBidForUser(conn, auction.getId(), ab.getUserId());
            outbidUser
                .getWallet()
                .setFrozenAmount(String.valueOf(auction.getId()), BigDecimal.valueOf(retainedBid));
            userDAO.update(conn, outbidUser);

            String msg =
                "Auto-bid của bạn cho phiên #"
                    + auction.getId()
                    + " đã bị vượt (mức tối đa: "
                    + ab.getMaxAmount()
                    + "). Mời bạn đặt lại!";
            app.server.network.Server.sendPacketToUser(
                ab.getUserId(), PacketRes.of(ResponseType.CHAT_MESSAGE, msg, null));

            WalletUpdateResponse walletUpdate =
                new WalletUpdateResponse(app.common.mapper.ModelMapper.toUserDto(outbidUser));
            app.server.network.Server.sendPacketToUser(
                ab.getUserId(), PacketRes.of(ResponseType.WALLET_UPDATED, "OK", walletUpdate));
          }
        } catch (Exception e) {
          // Log minimal warnings
        }
      }
    }

    return changed;
  }

  private Auction lockedRunningAuction(Connection conn, int auctionId) {
    auctionDAO.lockRow(conn, auctionId);
    Auction auction =
        auctionDAO
            .findById(conn, auctionId)
            .orElseThrow(() -> new ServiceException("Phiên đấu giá không tồn tại."));
    bidValidator.validateAuctionState(auction);
    return auction;
  }

  private void validateAutoBidActor(int auctionId, User actor) {
    if (auctionId <= 0) {
      throw new ServiceException("Phiên đấu giá không hợp lệ.");
    }
    OwnershipGuard.requireValidActor(actor);
  }

  private void validateAutoBidRules(
      Auction auction, Item item, User actor, long maxAmount, long incrementAmount) {
    if (incrementAmount <= 0) {
      throw new ServiceException("Bước tăng auto-bid không hợp lệ.");
    }
    long stepPrice = item.getStepPrice();
    if (stepPrice <= 0) {
      throw new ServiceException("Bước giá không hợp lệ.");
    }
    long minimumMaxAmount = auction.getHighestBid();
    if (auction.getWinnerId() == null || auction.getWinnerId() != actor.getId()) {
      minimumMaxAmount += stepPrice;
    }
    if (maxAmount < minimumMaxAmount) {
      throw new ServiceException("Giá tối đa auto-bid phải từ " + minimumMaxAmount + " trở lên.");
    }
  }

  private Item auctionItem(Connection conn, Auction auction) {
    return itemDAO
        .findById(conn, auction.getItemId())
        .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
  }

  private User lockedUser(Connection conn, int userId) {
    userDAO.lockRow(conn, userId);
    return userDAO
        .findById(conn, userId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
  }

  private AutoBid saveAutoBid(
      Connection conn, int auctionId, int userId, long maxAmount, long incrementAmount) {
    return autoBidDAO.save(
        conn, new AutoBid(0, auctionId, userId, maxAmount, incrementAmount, true, null, null));
  }

  private long highestBidForUser(Connection conn, int auctionId, int userId) {
    long highest = 0;
    for (Bid bid : bidDAO.findByAuction(conn, auctionId)) {
      if (bid.getBidderId() == userId) {
        highest = Math.max(highest, bid.getAmount());
      }
    }
    return highest;
  }
}
