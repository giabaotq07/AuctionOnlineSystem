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
import app.server.service.result.AuctionCompletion;
import app.server.service.result.AuctionSettlementResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Write-side auction operations. */
public class AuctionService {
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final ItemDAO itemDAO;
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;
  private final AuctionSettlementService settlementService;
  private final Clock clock;
  private final Logger logger = LoggerFactory.getLogger(AuctionService.class);

  public AuctionService(
      AuctionDAO auctionDAO,
      BidDAO bidDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      TransactionManager transactionManager,
      AuctionSettlementService settlementService,
      Clock clock) {
    this.auctionDAO = Objects.requireNonNull(auctionDAO, "auctionDAO");
    this.bidDAO = Objects.requireNonNull(bidDAO, "bidDAO");
    this.itemDAO = Objects.requireNonNull(itemDAO, "itemDAO");
    this.userDAO = Objects.requireNonNull(userDAO, "userDAO");
    this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager");
    this.settlementService = Objects.requireNonNull(settlementService, "settlementService");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public Auction createAuction(
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      app.common.enums.ItemType type,
      int durationMinutes,
      LocalDateTime startTime,
      User actor) {
    validateCreateInput(
        name, description, startingPrice, stepPrice, type, durationMinutes, startTime, actor);
    Auction auction = transactionManager.runInTransaction(
        conn -> {
          Item item = ItemFactory.createItem(
              name, actor.getId(), description, startingPrice, stepPrice, type);
          Item savedItem = itemDAO.save(conn, item);
          Auction created = new Auction(
              savedItem.getId(),
              actor.getId(),
              startTime.plusMinutes(durationMinutes),
              savedItem.getStartingPrice());
          created.setStartTime(startTime);
          Auction savedAuction = auctionDAO.save(conn, created);
          if (!startTime.isAfter(now())) {
            savedAuction.start();
            auctionDAO.update(conn, savedAuction);
          }
          return savedAuction;
        });
    return auction;
  }

  public Auction updateAuction(
      int auctionId,
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      app.common.enums.ItemType type,
      int durationMinutes,
      LocalDateTime startTime,
      int expectedVersion,
      User actor) {
    validateUpdateInput(
        auctionId,
        name,
        description,
        startingPrice,
        stepPrice,
        type,
        durationMinutes,
        startTime,
        expectedVersion,
        actor);
    Auction auction = transactionManager.runInTransaction(
        conn -> {
          Auction storedAuction = requireAuction(conn, auctionId);
          ensureUpdatePermission(storedAuction, actor);
          if (storedAuction.getStatus() != AuctionStatus.OPEN) {
            throw new ServiceException("Chỉ có thể sửa phiên chưa bắt đầu.");
          }
          LocalDateTime now = now();
          ensureEditableStartTime(storedAuction.getStartTime(), now);
          ensureEditableStartTime(startTime, now);

          Item storedItem = itemDAO
              .findById(conn, storedAuction.getItemId())
              .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
          boolean startingPriceChanged = storedItem.getStartingPrice() != startingPrice;
          storedItem.setName(name);
          storedItem.setDescription(description);
          storedItem.setStartingPrice(startingPrice);
          storedItem.setStepPrice(stepPrice);
          storedItem.setType(type);
          itemDAO.update(conn, storedItem);

          storedAuction.setStartTime(startTime);
          storedAuction.setEndTime(startTime.plusMinutes(durationMinutes));
          if (startingPriceChanged) {
            storedAuction.setHighestBid(startingPrice);
          }
          boolean updated = auctionDAO.updateIfVersionMatches(conn, storedAuction, expectedVersion);
          if (!updated) {
            throw new ServiceException("Dữ liệu phiên đã thay đổi, vui lòng tải lại.");
          }
          return storedAuction;
        });
    return auction;
  }

  public Set<Integer> cancelAuction(int auctionId, User actor, int expectedVersion) {
    validateActor(actor);
    if (auctionId <= 0 || expectedVersion < 0) {
      throw new ServiceException("Dữ liệu phiên đấu giá không hợp lệ.");
    }
    return transactionManager.runInTransaction(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          AuctionStatus oldStatus = auction.getStatus();
          ensureCancelableStatus(oldStatus);
          User requester = requireUser(conn, actor.getId());
          ensureCancelPermission(auction, requester);

          auction.cancel();
          Set<Integer> affectedUserIds = Set.of();
          if (oldStatus == AuctionStatus.RUNNING) {
            affectedUserIds = settlementService.releaseWallets(conn, auction);
          }
          boolean updated = auctionDAO.updateIfVersionMatches(conn, auction, expectedVersion);
          if (!updated) {
            throw new ServiceException(
                "Dữ liệu phiên đã thay đổi, vui lòng tải lại trước khi duyệt.");
          }
          return affectedUserIds;
        });
  }

  public boolean startOpenAuction(int auctionId) {
    boolean[] updated = { false };
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          if (auction.getStatus() == AuctionStatus.OPEN
              && auction.getStartTime() != null
              && !auction.getStartTime().isAfter(now())) {
            auction.start();
            auctionDAO.update(conn, auction);
            updated[0] = true;
          }
        });
    return updated[0];
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
          logCompletion(auctionId, highestBid);
          AuctionSettlementResult settlement = settlementService.settleWalletsWithResult(conn, auction);
          if (auction.getWinnerId() != null && settlement.winningAmount().signum() > 0) {
            auction.markPaid();
          }
          auctionDAO.update(conn, auction);
          return new AuctionCompletion(
              auctionId, true, highestBid, settlement.winningAmount(), settlement.settledUserIds());
        });
  }

  public List<AuctionCompletion> completeExpiredAuctionCompletions() {
    List<AuctionCompletion> completions = new ArrayList<>();
    for (Auction auction : auctionDAO.findAll()) {
      if (!auction.isExpired(clock)) {
        continue;
      }
      AuctionStatus status = auction.getStatus();
      if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
        continue;
      }
      AuctionCompletion completion = completeAuction(auction.getId());
      if (completion.completed()) {
        completions.add(completion);
      }
    }
    return completions;
  }

  public BigDecimal settleAuctionPayment(int auctionId) {
    BigDecimal[] amountRef = { BigDecimal.ZERO };
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          if (auction.getStatus() == AuctionStatus.PAID) {
            return;
          }
          if (auction.getStatus() != AuctionStatus.FINISHED) {
            return;
          }
          AuctionSettlementResult settlement = settlementService.settleWalletsWithResult(conn, auction);
          BigDecimal winningAmount = settlement.winningAmount();
          if (auction.getWinnerId() != null && winningAmount.signum() > 0) {
            auction.markPaid();
            auctionDAO.update(conn, auction);
          }
          amountRef[0] = winningAmount;
        });
    return amountRef[0];
  }

  private void validateCreateInput(
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      ItemType type,
      int durationMinutes,
      LocalDateTime startTime,
      User actor) {
    validateActor(actor);
    if (actor.getRole() != UserRole.SELLER && actor.getRole() != UserRole.ADMIN) {
      throw new ServiceException("Chỉ Seller/Admin được tạo phiên.");
    }
    if (name == null
        || name.isBlank()
        || description == null
        || description.isBlank()
        || startingPrice <= 0
        || stepPrice <= 0
        || durationMinutes <= 0
        || type == null
        || startTime == null) {
      throw new ServiceException("Dữ liệu phiên đấu giá không hợp lệ.");
    }
  }

  private void validateUpdateInput(
      int auctionId,
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      app.common.enums.ItemType type,
      int durationMinutes,
      LocalDateTime startTime,
      int expectedVersion,
      User actor) {
    if (auctionId <= 0 || expectedVersion < 0) {
      throw new ServiceException("Dữ liệu phiên đấu giá không hợp lệ.");
    }
    validateCreateInput(
        name, description, startingPrice, stepPrice, type, durationMinutes, startTime, actor);
  }

  private void validateActor(User actor) {
    if (actor == null || actor.getId() <= 0 || actor.getRole() == null) {
      throw new ServiceException("Dữ liệu người dùng không hợp lệ.");
    }
  }

  private void ensureCancelableStatus(AuctionStatus status) {
    if (status == AuctionStatus.FINISHED
        || status == AuctionStatus.PAID
        || status == AuctionStatus.CANCELED) {
      throw new ServiceException("Không thể hủy phiên đã kết thúc hoặc đã hủy.");
    }
    if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
      throw new ServiceException("Không thể hủy phiên ở trạng thái hiện tại.");
    }
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

  private void ensureUpdatePermission(Auction auction, User actor) {
    if (actor.getRole() == UserRole.ADMIN) {
      return;
    }
    if (actor.getRole() == UserRole.SELLER && auction.getSellerId() == actor.getId()) {
      return;
    }
    throw new ServiceException("Bạn không có quyền sửa phiên đấu giá này.");
  }

  private void ensureEditableStartTime(LocalDateTime startTime, LocalDateTime now) {
    if (startTime == null || !startTime.isAfter(now.plusMinutes(1))) {
      throw new ServiceException("Chỉ có thể sửa phiên còn hơn 1 phút trước khi bắt đầu.");
    }
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

  private LocalDateTime now() {
    return LocalDateTime.now(clock);
  }

  private void logCompletion(int auctionId, Optional<Bid> highestBid) {
    highestBid.ifPresentOrElse(
        bid -> logger.info(
            "Phiên {} kết thúc. Winner: {}, Giá: {}",
            auctionId,
            bid.getBidderName(),
            bid.getAmount()),
        () -> logger.info("Phiên {} kết thúc. Không có bid.", auctionId));
  }
}
