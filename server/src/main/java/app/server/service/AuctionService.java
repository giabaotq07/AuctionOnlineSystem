package app.server.service;

import app.common.dto.AuctionSummary;
import app.common.enums.AuctionStatus;
import app.common.enums.ItemType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.models.*;
import app.server.dao.AuctionDAO;
import app.server.dao.BidDAO;
import app.server.dao.ItemDAO;
import app.server.dao.UserDAO;
import app.server.database.TransactionManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionService {
  private volatile List<AuctionSnapshot> snapshotCache;
  private final Object snapshotCacheLock = new Object();
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final ItemDAO itemDAO;
  private final TransactionManager transactionManager;
  private final AuctionSettlementService settlementService;
  private final AuctionQueryService queryService;
  private final AuctionCommandService commandService;
  private final Logger logger = LoggerFactory.getLogger(AuctionService.class);
  private final Clock clock;

  public AuctionService(
      AuctionDAO auctionDAO,
      BidDAO bidDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      TransactionManager transactionManager) {
    this(auctionDAO, bidDAO, itemDAO, userDAO, transactionManager, Clock.systemDefaultZone());
  }

  public AuctionService(
      AuctionDAO auctionDAO,
      BidDAO bidDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      TransactionManager transactionManager,
      Clock clock) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
    this.itemDAO = itemDAO;
    this.transactionManager = transactionManager;
    this.settlementService = new AuctionSettlementService(bidDAO, userDAO);
    this.queryService = new AuctionQueryService();
    this.clock = Objects.requireNonNull(clock, "clock");
    this.commandService =
        new AuctionCommandService(
            auctionDAO, bidDAO, itemDAO, userDAO, transactionManager, settlementService, clock);
  }

  public Auction createAndStartAuctionWithItem(
      String name,
      String description,
      long startingPrice,
      long stepPrice,
      ItemType type,
      int durationMinutes,
      int requesterId,
      UserRole requesterRole,
      LocalDateTime startTime) {
    AuctionCommandService.validateCreateAuctionRequest(
        name,
        description,
        startingPrice,
        stepPrice,
        type,
        durationMinutes,
        requesterId,
        requesterRole);
    Auction createdAuction =
        transactionManager.runInTransaction(
            conn -> {
              Item item =
                  ItemFactory.createItem(
                      name, requesterId, description, startingPrice, stepPrice, type);
              Item savedItem = itemDAO.save(conn, item);
              Auction auction =
                  new Auction(
                      savedItem.getId(),
                      requesterId,
                      startTime.plusMinutes(durationMinutes),
                      savedItem.getStartingPrice());
              auction.setStartTime(startTime);
              Auction created = auctionDAO.save(conn, auction);
              if (!startTime.isAfter(LocalDateTime.now(clock))) {
                created.start();
                auctionDAO.update(conn, created);
              }
              return created;
            });
    if (createdAuction.getStatus() == AuctionStatus.OPEN && createdAuction.getStartTime() != null) {
      AuctionScheduler.getInstance()
          .scheduleStart(createdAuction.getId(), createdAuction.getStartTime());
    }
    invalidateCache();
    return createdAuction;
  }

  public List<AuctionSnapshot> getAuctions() {
    List<AuctionSnapshot> cached = snapshotCache;
    if (cached != null) {
      return cached;
    }
    synchronized (snapshotCacheLock) {
      cached = snapshotCache;
      if (cached == null) {
        cached = auctionDAO.findAll().stream().map(this::toSnapshot).toList();
        snapshotCache = cached;
      }
      return cached;
    }
  }

  public List<AuctionSnapshot> getHistoryAuctions(int userId) {
    return queryService.filterHistorySnapshots(
        getAuctions(), snapshot -> isSellerOrBidder(snapshot.auction(), userId));
  }

  public List<AuctionSummary> getAuctionSummaries() {
    return queryService.toAuctionSummaries(getAuctions());
  }

  public List<AuctionSummary> getHistorySummaries(int userId) {
    return queryService.toAuctionSummaries(getHistoryAuctions(userId));
  }

  public AuctionSnapshot getAuction(int auctionId) {
    List<AuctionSnapshot> cached = snapshotCache;
    if (cached != null) {
      for (AuctionSnapshot snapshot : cached) {
        if (snapshot.auctionId() == auctionId) {
          return snapshot;
        }
      }
    }
    Auction auction =
        auctionDAO
            .findById(auctionId)
            .orElseThrow(() -> new ServiceException("Không tìm thấy phiên ID: " + auctionId));
    return toSnapshot(auction);
  }

  public boolean isAuctionVersionCurrent(int auctionId, int knownVersion) {
    if (knownVersion < 0) {
      return false;
    }
    return getAuction(auctionId).version() == knownVersion;
  }

  public Optional<Bid> completeAndGetHighestBid(int auctionId) {
    return completeAuction(auctionId).highestBid();
  }

  public Optional<Bid> findHighestBid(int auctionId) {
    return bidDAO.findHighestBid(auctionId);
  }

  public boolean completeAuctionIfExpired(int auctionId) {
    return completeAuction(auctionId).completed();
  }

  public void cancelAuction(int auctionId, int requester, int expectedVersion) {
    commandService.cancelAuction(auctionId, requester, expectedVersion);
    invalidateCache();
  }

  public boolean startOpenAuction(int auctionId) {
    boolean[] updated = new boolean[] {false};
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          if (auction.getStatus() == AuctionStatus.OPEN) {
            auction.start();
            auctionDAO.update(conn, auction);
            updated[0] = true;
          }
        });
    if (updated[0]) {
      invalidateCache();
    }
    return updated[0];
  }

  public AuctionCompletion completeAuction(int auctionId) {
    AuctionCompletion completion = commandService.completeAuction(auctionId);
    if (completion.completed()) {
      invalidateCache();
    }
    return completion;
  }

  public List<Integer> completeExpiredAuctions() {
    return completeExpiredAuctionCompletions().stream().map(AuctionCompletion::auctionId).toList();
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

  public void invalidateCache() {
    synchronized (snapshotCacheLock) {
      snapshotCache = null;
    }
    logger.info("[CACHE] Auction cache invalidated");
  }

  private boolean isSellerOrBidder(Auction auction, int userId) {
    return auction.getSellerId() == userId
        || bidDAO.existsByAuctionAndUser(auction.getId(), userId);
  }

  private AuctionSnapshot toSnapshot(Auction auction) {
    Item item =
        itemDAO
            .findById(auction.getItemId())
            .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
    return new AuctionSnapshot(auction, item);
  }

  private Auction requireAuction(java.sql.Connection conn, int auctionId) {
    auctionDAO.lockRow(conn, auctionId);
    return auctionDAO
        .findById(conn, auctionId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
  }

  public BigDecimal settleAuctionPayment(int auctionId) {
    BigDecimal[] amountRef = new BigDecimal[] {BigDecimal.ZERO};
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
            auction.setStatus(AuctionStatus.PAID);
            auctionDAO.update(conn, auction);
          }
          amountRef[0] = winningAmount;
        });
    if (amountRef[0].signum() > 0) {
      invalidateCache();
    }
    return amountRef[0];
  }
}
