package app.server.service;

import app.common.dto.AuctionPreview;
import app.common.enums.AuctionStatus;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.Bid;
import app.common.models.Item;
import app.common.models.User;
import app.server.dao.AuctionDAO;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import java.util.List;
import java.util.Objects;

/** Read-only auction operations that assemble domain aggregates. */
public class AuctionQueryService {
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final ItemDAO itemDAO;
  private final UserDAO userDAO;

  public AuctionQueryService(
      AuctionDAO auctionDAO, BidDAO bidDAO, ItemDAO itemDAO, UserDAO userDAO) {
    this.auctionDAO = Objects.requireNonNull(auctionDAO, "auctionDAO");
    this.bidDAO = Objects.requireNonNull(bidDAO, "bidDAO");
    this.itemDAO = Objects.requireNonNull(itemDAO, "itemDAO");
    this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
  }

  public List<Auction> getAuctions() {
    return auctionDAO.findAll().stream().map(auction -> toAggregate(auction, false)).toList();
  }

  public List<AuctionPreview> getAuctionPreviews() {
    return getAuctions().stream().map(AuctionPreview::from).toList();
  }

  public Auction getAuction(int auctionId) {
    Auction auction =
        auctionDAO
            .findById(auctionId)
            .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
    return toAggregate(auction, false);
  }

  public Auction getAuctionDetail(int auctionId) {
    Auction auction =
        auctionDAO
            .findById(auctionId)
            .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
    return toAggregate(auction, true);
  }

  public boolean isAuctionVersionCurrent(int auctionId, int knownVersion) {
    if (knownVersion < 0) {
      return false;
    }
    return getAuction(auctionId).getVersion() == knownVersion;
  }

  public List<Auction> getHistoryAuctions(int userId) {
    return getAuctions().stream()
        .filter(auction -> isHistoryStatus(auction.getStatus()))
        .filter(auction -> isSellerOrBidder(auction, userId))
        .toList();
  }

  public List<AuctionPreview> getHistoryAuctionPreviews(int userId) {
    return getHistoryAuctions(userId).stream().map(AuctionPreview::from).toList();
  }

  public List<Auction> getAuctionsByItem(int itemId) {
    return getAuctions().stream()
        .filter(auction -> auction.getItem() != null && auction.getItem().getId() == itemId)
        .toList();
  }

  private Auction toAggregate(Auction auction, boolean includeBids) {
    Item item =
        itemDAO
            .findById(auction.getItemId())
            .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
    User seller = publicUser(auction.getSellerId());
    item.setSeller(seller);
    auction.setItem(item);
    auction.setSeller(seller);
    Integer winnerId = auction.getWinnerId();
    if (winnerId != null) {
      auction.setWinner(publicUser(winnerId));
    }
    if (includeBids) {
      auction.setBids(
          bidDAO.findByAuctionOrderByTime(auction.getId()).stream().map(this::withBidder).toList());
    }
    return auction;
  }

  private Bid withBidder(Bid bid) {
    userDAO.findById(bid.getBidderId()).map(User::publicView).ifPresent(bid::setBidder);
    return bid;
  }

  private User publicUser(int userId) {
    return userDAO
        .findById(userId)
        .map(User::publicView)
        .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
  }

  private boolean isSellerOrBidder(Auction auction, int userId) {
    return auction.getSellerId() == userId
        || bidDAO.existsByAuctionAndUser(auction.getId(), userId);
  }

  private boolean isHistoryStatus(AuctionStatus status) {
    return status == AuctionStatus.FINISHED
        || status == AuctionStatus.PAID
        || status == AuctionStatus.CANCELED;
  }
}
