package app.server.service;

import app.common.enums.AuctionStatus;
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
    validateAuctionManager(actor);
    validateAuctionPayload(name, startingPrice, stepPrice, type, durationMinutes, startTime);

    Auction auction =
        transactionManager.runInTransaction(
            conn -> {
              Item item =
                  ItemFactory.createItem(
                      name, actor.getId(), description, startingPrice, stepPrice, type);
              Item savedItem = itemDAO.save(conn, item);
              Auction created =
                  new Auction(
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
    validateAuctionIdentity(auctionId, expectedVersion);
    validateAuctionManager(actor);
    validateAuctionPayload(name, startingPrice, stepPrice, type, durationMinutes, startTime);

    Auction auction =
        transactionManager.runInTransaction(
            conn -> {
              Auction storedAuction = requireAuction(conn, auctionId);
              validateOwnerOrAdmin(storedAuction, actor);
              validateEditableAuction(storedAuction);

              Item storedItem =
                  itemDAO
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
              boolean updated =
                  auctionDAO.updateIfVersionMatches(conn, storedAuction, expectedVersion);
              if (!updated) {
                throw new ServiceException("Dữ liệu phiên đã thay đổi, vui lòng tải lại.");
              }
              return storedAuction;
            });
    return auction;
  }

  public Set<Integer> cancelAuction(int auctionId, User actor, int expectedVersion) {
    validateAuctionIdentity(auctionId, expectedVersion);
    validateAuctionManager(actor);

    return transactionManager.runInTransaction(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          validateOwnerOrAdmin(auction, actor);
          validateCancelableAuction(auction);
          AuctionStatus oldStatus = auction.getStatus();

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
    boolean[] updated = {false};
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
    BigDecimal[] amountRef = {BigDecimal.ZERO};
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          if (auction.getStatus() == AuctionStatus.PAID) {
            return;
          }
          if (auction.getStatus() != AuctionStatus.FINISHED) {
            return;
          }
          AuctionSettlementResult settlement =
              settlementService.settleWalletsWithResult(conn, auction);
          BigDecimal winningAmount = settlement.winningAmount();
          if (auction.getWinnerId() != null && winningAmount.signum() > 0) {
            auction.markPaid();
            auctionDAO.update(conn, auction);
          }
          amountRef[0] = winningAmount;
        });
    return amountRef[0];
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

  private void validateAuctionManager(User actor) {
    if (actor == null || actor.getId() <= 0) {
      throw new ServiceException("Người dùng không hợp lệ.");
    }
    if (actor.getRole() != UserRole.SELLER && actor.getRole() != UserRole.ADMIN) {
      throw new ServiceException("Bạn không có quyền thực hiện yêu cầu này.");
    }
  }

  private void validateAuctionIdentity(int auctionId, int expectedVersion) {
    if (auctionId <= 0) {
      throw new ServiceException("Phiên đấu giá không hợp lệ.");
    }
    if (expectedVersion < 0) {
      throw new ServiceException("Phiên bản dữ liệu không hợp lệ.");
    }
  }

  private void validateAuctionPayload(
      String name,
      long startingPrice,
      long stepPrice,
      app.common.enums.ItemType type,
      int durationMinutes,
      LocalDateTime startTime) {
    if (name == null || name.isBlank()) {
      throw new ServiceException("Tên sản phẩm không được để trống.");
    }
    if (type == null) {
      throw new ServiceException("Loại sản phẩm không hợp lệ.");
    }
    if (startingPrice <= 0) {
      throw new ServiceException("Giá khởi điểm phải lớn hơn 0.");
    }
    if (stepPrice <= 0) {
      throw new ServiceException("Bước giá phải lớn hơn 0.");
    }
    if (durationMinutes <= 0) {
      throw new ServiceException("Thời lượng phiên phải lớn hơn 0.");
    }
    if (startTime == null) {
      throw new ServiceException("Thời gian bắt đầu không hợp lệ.");
    }
    if (!startTime.plusMinutes(durationMinutes).isAfter(now())) {
      throw new ServiceException("Thời gian kết thúc phải sau thời điểm hiện tại.");
    }
  }

  private void validateOwnerOrAdmin(Auction auction, User actor) {
    if (actor.getRole() == UserRole.ADMIN) {
      return;
    }
    if (auction.getSellerId() != actor.getId()) {
      throw new ServiceException("Bạn không có quyền thực hiện yêu cầu này.");
    }
  }

  private void validateEditableAuction(Auction auction) {
    if (auction.getStatus() != AuctionStatus.OPEN) {
      throw new ServiceException("Chỉ có thể cập nhật phiên chưa bắt đầu.");
    }
  }

  private void validateCancelableAuction(Auction auction) {
    if (auction.getStatus() != AuctionStatus.OPEN && auction.getStatus() != AuctionStatus.RUNNING) {
      throw new ServiceException("Chỉ có thể hủy phiên đang mở hoặc đang chạy.");
    }
  }

  private void logCompletion(int auctionId, Optional<Bid> highestBid) {
    highestBid.ifPresentOrElse(
        bid ->
            logger.info(
                "Phiên {} kết thúc. Winner: {}, Giá: {}",
                auctionId,
                bid.getBidderName(),
                bid.getAmount()),
        () -> logger.info("Phiên {} kết thúc. Không có bid.", auctionId));
  }
}
