package app.service;

import app.DAO.AuctionDAO;
import app.DAO.BidDAO;
import app.DAO.ItemDAO;
import app.DAO.UserDAO;
import app.data.AuctionDetail;
import app.data.AuctionResultResponse;
import app.data.AuctionSummary;
import app.data.ProfileData;
import app.database.TransactionManager;
import app.enums.AuctionStatus;
import app.exception.ServiceException;
import app.models.Auction;
import app.models.BidTransaction;
import app.models.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** AuctionService. */
public class AuctionService {
  private record CacheSnapshot(List<Auction> data, boolean loaded) {}

  private record SummaryCacheSnapshot(List<AuctionSummary> data, boolean loaded) {}

  private volatile CacheSnapshot snapshot = new CacheSnapshot(List.of(), false);
  private volatile SummaryCacheSnapshot summarySnapshot =
      new SummaryCacheSnapshot(List.of(), false);
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final UserDAO userDAO;
  private final TransactionManager transactionManager;
  private final AuctionMapper auctionMapper;
  private final Logger logger = LoggerFactory.getLogger(AuctionService.class);
  private final Clock clock;

  /** AuctionService. */
  public AuctionService(
      AuctionDAO auctionDAO,
      BidDAO bidDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      TransactionManager transactionManager) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
    this.userDAO = userDAO;
    this.auctionMapper = new AuctionMapper(itemDAO, bidDAO);
    this.transactionManager = transactionManager;
    this.clock = Clock.systemDefaultZone();
  }

  /** createAuction. */
  public Auction createAuction(Auction auction) {
    validateAuctionTime(auction);
    Auction saved = transactionManager.runInTransaction(conn -> auctionDAO.save(conn, auction));
    invalidateCache();
    return saved;
  }

  /** createAndStartAuction. */
  public Auction createAndStartAuction(int itemId, int sellerId, long startingPrice, int minutes) {
    Auction saved =
        transactionManager.runInTransaction(
            conn -> {
              Auction auction =
                  new Auction(
                      itemId,
                      sellerId,
                      LocalDateTime.now(clock).plusMinutes(minutes),
                      startingPrice);
              Auction created = auctionDAO.save(conn, auction);
              created.start();
              auctionDAO.update(conn, created);
              return created;
            });
    invalidateCache();
    return saved;
  }

  /** getAuctionById. */
  public Auction getAuctionById(int auctionId) {
    return auctionDAO
        .findById(auctionId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên ID: " + auctionId));
  }

  /** getAllAuctions. */
  public List<Auction> getAllAuctions() {
    CacheSnapshot current = snapshot;
    if (current.loaded()) {
      return current.data();
    }
    List<Auction> fresh = auctionDAO.findAll();
    snapshot = new CacheSnapshot(List.copyOf(fresh), true);
    return fresh;
  }

  /** getAuctionSummaries. */
  public List<AuctionSummary> getAuctionSummaries() {
    SummaryCacheSnapshot current = summarySnapshot;
    if (current.loaded()) {
      logger.info("[CACHE] Auction summaries cache hit: size={}", current.data().size());
      return current.data();
    }
    logger.info("[CACHE] Auction summaries cache miss, loading from database");
    List<AuctionSummary> result = new ArrayList<>();
    for (Auction auction : getAllAuctions()) {
      AuctionSummary summary = auctionMapper.toSummary(auction);
      if (summary != null) {
        result.add(summary);
      }
    }
    List<AuctionSummary> cached = List.copyOf(result);
    summarySnapshot = new SummaryCacheSnapshot(cached, true);
    return cached;
  }

  /** getAllAuctionSummariesForAdmin. */
  public List<AuctionSummary> getAllAuctionSummariesForAdmin(int userId) {
    requireAdmin(userId);
    return getAuctionSummaries();
  }

  /** getHistorySummaries. */
  public List<AuctionSummary> getHistorySummaries(int userId) {
    List<AuctionSummary> result = new ArrayList<>();
    for (Auction auction : getAllAuctions()) {
      boolean isSeller = auction.getSellerId() == userId;
      boolean hasBid = bidDAO.existsByAuctionAndUser(auction.getId(), userId);
      if (!isSeller && !hasBid) {
        continue;
      }
      AuctionSummary summary = auctionMapper.toSummary(auction);
      if (summary != null) {
        result.add(summary);
      }
    }
    return result;
  }

  /** getAuctionDetail. */
  public AuctionDetail getAuctionDetail(int auctionId) {
    Auction auction = getAuctionById(auctionId);
    return auctionMapper.toDetail(auction);
  }

  /** getAuctionResult. */
  public AuctionResultResponse getAuctionResult(int auctionId) {
    handleCompletion(auctionId);
    Optional<BidTransaction> highest = bidDAO.findHighestBid(auctionId);
    if (highest.isEmpty()) {
      return new AuctionResultResponse(
          true, auctionId, new ProfileData(0, "chưa có người thắng"), 0);
    }
    BidTransaction bid = highest.get();
    return new AuctionResultResponse(
        true, auctionId, new ProfileData(bid.getBidderId(), bid.getBidderName()), bid.getAmount());
  }

  /** updateStatus. */
  public void updateStatus(int auctionId, AuctionStatus status) {
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          auction.setStatus(status);
          boolean ok = auctionDAO.update(conn, auction);
          if (!ok) {
            throw new ServiceException("Không thể cập nhật trạng thái.");
          }
        });
    invalidateCache();
  }

  /** cancelAuctionByAdmin. */
  public void cancelAuctionByAdmin(int auctionId, int adminId, int expectedVersion) {
    requireAdmin(adminId);
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          AuctionStatus status = auction.getStatus();
          if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID) {
            throw new ServiceException("Không thể hủy phiên đã kết thúc hoặc đã thanh toán.");
          }
          auction.setStatus(AuctionStatus.CANCELED);
          releaseWallets(conn, auction);
          boolean ok = auctionDAO.updateIfVersionMatches(conn, auction, expectedVersion);
          if (!ok) {
            throw new ServiceException(
                "Dữ liệu phiên đã thay đổi, vui lòng tải lại trước khi duyệt.");
          }
        });
    invalidateCache();
  }

  /** setStartTime. */
  public void setStartTime(int auctionId, LocalDateTime startTime) {
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          auction.setStartTime(startTime);
          auctionDAO.update(conn, auction);
        });
    invalidateCache();
  }

  /** setEndTime. */
  public void setEndTime(int auctionId, LocalDateTime endTime) {
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          auction.setEndTime(endTime);
          auctionDAO.update(conn, auction);
        });
    invalidateCache();
  }

  /** handleCompletion. */
  public void handleCompletion(int auctionId) {
    boolean completed =
        transactionManager.runInTransaction(
            conn -> {
              Auction auction = requireAuction(conn, auctionId);
              if (!auction.isExpired()) {
                return false;
              }
              AuctionStatus status = auction.getStatus();
              if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
                return false;
              }
              auction.setStatus(AuctionStatus.FINISHED);
              bidDAO
                  .findHighestBid(conn, auctionId)
                  .ifPresentOrElse(
                      bid -> {
                        auction.setWinnerId(bid.getBidderId());
                        logger.info(
                            "Phiên {} kết thúc. Winner: {}, Giá: {}",
                            auctionId,
                            bid.getBidderName(),
                            bid.getAmount());
                      },
                      () -> logger.info("Phiên {} kết thúc. Không có bid.", auctionId));
              settleWallets(conn, auction);
              auctionDAO.update(conn, auction);
              return true;
            });
    if (completed) {
      invalidateCache();
    }
  }

  /** completeExpiredAuctions. */
  public List<Integer> completeExpiredAuctions() {
    List<Integer> completedIds = new ArrayList<>();
    for (Auction auction : auctionDAO.findAll()) {
      if (!auction.isExpired()) {
        continue;
      }
      AuctionStatus status = auction.getStatus();
      if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
        continue;
      }
      handleCompletion(auction.getId());
      completedIds.add(auction.getId());
    }
    return completedIds;
  }

  private Auction requireAuction(java.sql.Connection conn, int auctionId) {
    auctionDAO.lockRow(conn, auctionId);
    return auctionDAO
        .findById(conn, auctionId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
  }

  private void settleWallets(java.sql.Connection conn, Auction auction) {
    List<BidTransaction> bids = bidDAO.findByAuction(conn, auction.getId());
    Set<Integer> bidderIds = new LinkedHashSet<>();
    for (BidTransaction bid : bids) {
      bidderIds.add(bid.getBidderId());
    }
    for (Integer bidderId : bidderIds) {
      userDAO.lockRow(conn, bidderId);
      User user =
          userDAO
              .findById(conn, bidderId)
              .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + bidderId));
      if (auction.getWinnerId() != null && auction.getWinnerId() == bidderId) {
        user.getWallet().commitFrozen(String.valueOf(auction.getId()));
      } else {
        user.getWallet().releaseFrozen(String.valueOf(auction.getId()));
      }
      userDAO.update(conn, user);
    }
  }

  private void releaseWallets(java.sql.Connection conn, Auction auction) {
    List<BidTransaction> bids = bidDAO.findByAuction(conn, auction.getId());
    Set<Integer> bidderIds = new LinkedHashSet<>();
    for (BidTransaction bid : bids) {
      bidderIds.add(bid.getBidderId());
    }
    for (Integer bidderId : bidderIds) {
      userDAO.lockRow(conn, bidderId);
      User user =
          userDAO
              .findById(conn, bidderId)
              .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + bidderId));
      user.getWallet().releaseFrozen(String.valueOf(auction.getId()));
      userDAO.update(conn, user);
    }
  }

  private void requireAdmin(int userId) {
    User user =
        userDAO
            .findById(userId)
            .orElseThrow(() -> new ServiceException("Không tìm thấy user với id: " + userId));
    if (user.getRole() != app.enums.UserRole.ADMIN) {
      throw new ServiceException("Chỉ Admin được thực hiện thao tác này.");
    }
  }

  private void validateAuctionTime(Auction auction) {
    if (auction.getEndTime().isBefore(LocalDateTime.now(clock))) {
      throw new ServiceException("Thời gian kết thúc không thể ở quá khứ.");
    }
  }

  /** invalidateCache. */
  public void invalidateCache() {
    snapshot = new CacheSnapshot(List.of(), false);
    summarySnapshot = new SummaryCacheSnapshot(List.of(), false);
    logger.info("[CACHE] Auction cache invalidated");
  }
}
