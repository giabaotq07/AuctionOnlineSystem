package app.server.service;

import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.models.Bid;
import app.common.models.Item;
import app.common.models.ItemFactory;
import app.common.models.User;
import app.server.dao.AuctionDAO;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.database.TransactionManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Write-side auction operations. */
public class AuctionCommandService {
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final ItemDAO itemDAO;
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;
  private final AuctionSettlementService settlementService;
  private final Logger logger = LoggerFactory.getLogger(AuctionCommandService.class);
  private final Clock clock;

  public AuctionCommandService(
      AuctionDAO auctionDAO,
      BidDAO bidDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      TransactionManager transactionManager,
      AuctionSettlementService settlementService,
      Clock clock) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
    this.itemDAO = itemDAO;
    this.userDAO = userDAO;
    this.transactionManager = transactionManager;
    this.settlementService = settlementService;
    this.clock = clock;
  }

  public Auction createAndStartAuctionWithItem(
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      ItemType type,
      int durationMinutes,
      int requesterId,
      UserRole requesterRole) {
    validateCreateAuctionRequest(
        name,
        description,
        startingPrice,
        stepPrice,
        type,
        durationMinutes,
        requesterId,
        requesterRole);
    return transactionManager.runInTransaction(
        conn -> {
          Item item =
              ItemFactory.createItem(
                  name, requesterId, description, startingPrice, stepPrice, type);
          Item savedItem = itemDAO.save(conn, item);
          Auction auction =
              new Auction(
                  savedItem.getId(),
                  requesterId,
                  LocalDateTime.now(clock).plusMinutes(durationMinutes),
                  savedItem.getStartingPrice());
          Auction created = auctionDAO.save(conn, auction);
          created.start();
          auctionDAO.update(conn, created);
          return created;
        });
  }

  public Set<Integer> cancelAuction(int auctionId, int requester, int expectedVersion) {
    return transactionManager.runInTransaction(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          AuctionStatus status = auction.getStatus();
          if (status == AuctionStatus.FINISHED
              || status == AuctionStatus.PAID
              || status == AuctionStatus.CANCELED) {
            throw new ServiceException("Không thể hủy phiên đã kết thúc hoặc đã hủy.");
          }
          if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
            throw new ServiceException("Không thể hủy phiên ở trạng thái hiện tại.");
          }
          User requesterUser = requireUser(conn, requester);
          ensureCancelPermission(auction, requesterUser);
          auction.cancel();
          Set<Integer> releasedUserIds = Set.of();
          if (status == AuctionStatus.RUNNING) {
            releasedUserIds = settlementService.releaseWallets(conn, auction);
          }
          boolean ok = auctionDAO.updateIfVersionMatches(conn, auction, expectedVersion);
          if (!ok) {
            throw new ServiceException(
                "Dữ liệu phiên đã thay đổi, vui lòng tải lại trước khi duyệt.");
          }
          return releasedUserIds;
        });
  }

  public Auction updateAuctionWithItem(
      int auctionId,
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      ItemType type,
      int durationMinutes,
      LocalDateTime startTime,
      int requesterId,
      UserRole requesterRole,
      int expectedVersion) {
    validateUpdateAuctionRequest(
        auctionId,
        name,
        description,
        startingPrice,
        stepPrice,
        type,
        durationMinutes,
        startTime,
        requesterId,
        requesterRole,
        expectedVersion);
    return transactionManager.runInTransaction(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          ensureUpdatePermission(auction, requesterId, requesterRole);
          if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new ServiceException("Chỉ có thể sửa phiên chưa bắt đầu.");
          }
          LocalDateTime now = LocalDateTime.now(clock);
          ensureEditableStartTime(auction.getStartTime(), now);
          ensureEditableStartTime(startTime, now);
          Item stored =
              itemDAO
                  .findById(conn, auction.getItemId())
                  .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
          boolean startingPriceChanged = stored.getStartingPrice() != startingPrice;
          stored.setName(name);
          stored.setDescription(description);
          stored.setStartingPrice(startingPrice);
          stored.setStepPrice(stepPrice);
          stored.setType(type);
          itemDAO.update(conn, stored);
          auction.setStartTime(startTime);
          auction.setEndTime(startTime.plusMinutes(durationMinutes));
          if (startingPriceChanged) {
            auction.setHighestBid(startingPrice);
          }
          boolean ok = auctionDAO.updateIfVersionMatches(conn, auction, expectedVersion);
          if (!ok) {
            throw new ServiceException("Dữ liệu phiên đã thay đổi, vui lòng tải lại.");
          }
          return auction;
        });
  }

  public AuctionCompletion completeAuction(int auctionId) {
    return transactionManager.runInTransaction(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          Optional<Bid> highestBid = bidDAO.findHighestBid(conn, auctionId);
          if (!auction.isExpired(clock)) {
            return new AuctionCompletion(auctionId, false, highestBid, BigDecimal.ZERO, Set.of());
          }
          AuctionStatus status = auction.getStatus();
          if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
            return new AuctionCompletion(auctionId, false, highestBid, BigDecimal.ZERO, Set.of());
          }
          auction.finish(highestBid.map(Bid::getBidderId).orElse(null));
          highestBid.ifPresentOrElse(
              bid ->
                  logger.info(
                      "Phiên {} kết thúc. Winner: {}, Giá: {}",
                      auctionId,
                      bid.getBidderName(),
                      bid.getAmount()),
              () -> logger.info("Phiên {} kết thúc. Không có bid."));
          AuctionSettlementResult settlement =
              settlementService.settleWalletsWithResult(conn, auction);
          if (auction.getWinnerId() != null && settlement.winningAmount().signum() > 0) {
            auction.markPaid();
          }
          auctionDAO.update(conn, auction);
          return new AuctionCompletion(
              auctionId, true, highestBid, settlement.winningAmount(), settlement.settledUserIds());
        });
  }

  private Auction requireAuction(java.sql.Connection conn, int auctionId) {
    auctionDAO.lockRow(conn, auctionId);
    return auctionDAO
        .findById(conn, auctionId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
  }

  private User requireUser(java.sql.Connection conn, int userId) {
    return userDAO
        .findById(conn, userId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
  }

  private void ensureCancelPermission(Auction auction, User requester) {
    boolean isOwner = requester.getId() == auction.getSellerId();
    boolean isAdmin = requester.getRole() == UserRole.ADMIN;
    if (auction.getStatus() == AuctionStatus.OPEN && (isOwner || isAdmin)) {
      return;
    }
    if (auction.getStatus() == AuctionStatus.RUNNING && isAdmin) {
      return;
    }
    throw new ServiceException("Bạn không có quyền hủy phiên đấu giá này.");
  }

  private void ensureUpdatePermission(Auction auction, int requesterId, UserRole requesterRole) {
    if (requesterRole == UserRole.ADMIN) {
      return;
    }
    if (requesterRole == UserRole.SELLER && auction.getSellerId() == requesterId) {
      return;
    }
    throw new ServiceException("Bạn không có quyền sửa phiên đấu giá này.");
  }

  static void validateCreateAuctionRequest(
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      ItemType type,
      int durationMinutes,
      int requesterId,
      UserRole requesterRole) {
    if (requesterId <= 0 || requesterRole == null) {
      throw new ServiceException("Dữ liệu người tạo phiên không hợp lệ.");
    }
    if (requesterRole != UserRole.SELLER && requesterRole != UserRole.ADMIN) {
      throw new ServiceException("Chỉ Seller/Admin được tạo phiên.");
    }
    if (name == null
        || name.isBlank()
        || description == null
        || description.isBlank()
        || startingPrice <= 0
        || stepPrice <= 0
        || durationMinutes <= 0
        || type == null) {
      throw new ServiceException("Dữ liệu phiên đấu giá không hợp lệ.");
    }
  }

  private static void validateUpdateAuctionRequest(
      int auctionId,
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      ItemType type,
      int durationMinutes,
      LocalDateTime startTime,
      int requesterId,
      UserRole requesterRole,
      int expectedVersion) {
    if (auctionId <= 0 || expectedVersion < 0 || startTime == null) {
      throw new ServiceException("Dữ liệu phiên đấu giá không hợp lệ.");
    }
    validateCreateAuctionRequest(
        name,
        description,
        startingPrice,
        stepPrice,
        type,
        durationMinutes,
        requesterId,
        requesterRole);
  }

  private static void ensureEditableStartTime(LocalDateTime startTime, LocalDateTime now) {
    if (startTime == null || !startTime.isAfter(now.plusMinutes(1))) {
      throw new ServiceException("Chỉ có thể sửa phiên còn hơn 1 phút trước khi bắt đầu.");
    }
  }
}
