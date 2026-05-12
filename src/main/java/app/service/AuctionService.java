package app.service;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.data.AuctionDetail;
import app.data.AuctionResultResponse;
import app.data.AuctionSummary;
import app.data.ProfileData;
import app.database.TransactionManager;
import app.enums.AuctionStatus;
import app.exception.ServiceException;
import app.models.Auction;
import app.models.BidTransaction;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionService {
  private record CacheSnapshot(List<Auction> data, long timestamp) {}

  private static final long CACHE_DURATION_MS = 2000;
  private volatile CacheSnapshot snapshot = new CacheSnapshot(List.of(), 0L);
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final ItemDAO itemDAO;
  private final TransactionManager transactionManager;
  private final AuctionMapper auctionMapper;
  private final Logger logger = LoggerFactory.getLogger(AuctionService.class);
  private final Clock clock;

  public AuctionService(
      AuctionDAO auctionDAO,
      BidDAO bidDAO,
      ItemDAO itemDAO) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
    this.itemDAO = itemDAO;
    this.auctionMapper = new AuctionMapper(itemDAO,  bidDAO);
    this.transactionManager = new TransactionManager();
    this.clock = Clock.systemDefaultZone();
  }

  // ─────────────────────────────────────────────────────────────
  // CREATE
  // ─────────────────────────────────────────────────────────────
  public Auction createAuction(Auction auction) {
    validateAuctionTime(auction);
    return transactionManager.runInTransaction(conn -> auctionDAO.save(conn, auction));
  }

  public Auction createAndStartAuction(int itemId, int sellerId, long startingPrice, int minutes) {
    return transactionManager.runInTransaction(
        conn -> {
          Auction auction =
              new Auction(
                  itemId, sellerId, LocalDateTime.now(clock).plusMinutes(minutes), startingPrice);
          Auction saved = auctionDAO.save(conn, auction);
          saved.start();
          auctionDAO.update(conn, saved);
          return saved;
        });
  }

  // ─────────────────────────────────────────────────────────────
  // QUERY
  // ─────────────────────────────────────────────────────────────
  public Auction getAuctionById(int auctionId) {
    return auctionDAO
        .findById(auctionId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên ID: " + auctionId));
  }

  public List<Auction> getAllAuctions() {
    CacheSnapshot current = snapshot;
    if (isCacheValid(current)) {
      return current.data();
    }
    List<Auction> fresh = auctionDAO.findAll();
    snapshot = new CacheSnapshot(List.copyOf(fresh), System.currentTimeMillis());
    return fresh;
  }

  public List<AuctionSummary> getAuctionSummaries() {
    List<AuctionSummary> result = new ArrayList<>();
    for (Auction auction : auctionDAO.findAll()) {
      AuctionSummary summary = auctionMapper.toSummary(auction);
      if (summary != null) {
        result.add(summary);
      }
    }
    return result;
  }

  public List<AuctionSummary> getHistorySummaries(int userId) {
    List<AuctionSummary> result = new ArrayList<>();
    for (Auction auction : auctionDAO.findAll()) {
      boolean isSeller = auction.getSellerId() == userId;
      boolean hasBid = bidDAO.existsBySessionAndUser(auction.getId(), userId);
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

  public AuctionDetail getAuctionDetail(int auctionId) {
    Auction auction = getAuctionById(auctionId);
    return auctionMapper.toDetail(auction);
  }

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

  // ─────────────────────────────────────────────────────────────
  // UPDATE
  // ─────────────────────────────────────────────────────────────
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
  }

  public void setStartTime(int auctionId, LocalDateTime startTime) {
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          auction.setStartTime(startTime);
          auctionDAO.update(conn, auction);
        });
  }

  public void setEndTime(int auctionId, LocalDateTime endTime) {
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          auction.setEndTime(endTime);
          auctionDAO.update(conn, auction);
        });
  }

  // ─────────────────────────────────────────────────────────────
  // COMPLETION
  // ─────────────────────────────────────────────────────────────
  public void handleCompletion(int auctionId) {
    transactionManager.runWithoutResult(
        conn -> {
          Auction auction = requireAuction(conn, auctionId);
          if (!auction.isExpired()) {
            return;
          }
          AuctionStatus status = auction.getStatus();
          if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
            return;
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
          auctionDAO.update(conn, auction);
        });
  }

  // ─────────────────────────────────────────────────────────────
  // PRIVATE
  // ─────────────────────────────────────────────────────────────
  private Auction requireAuction(java.sql.Connection conn, int auctionId) {
    auctionDAO.lockRow(conn, auctionId);
    return auctionDAO
        .findById(conn, auctionId)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
  }

  private void validateAuctionTime(Auction auction) {
    if (auction.getEndTime().isBefore(LocalDateTime.now(clock))) {
      throw new ServiceException("Thời gian kết thúc không thể ở quá khứ.");
    }
  }

  private boolean isCacheValid(CacheSnapshot snapshot) {
    return System.currentTimeMillis() - snapshot.timestamp() < CACHE_DURATION_MS;
  }
}
