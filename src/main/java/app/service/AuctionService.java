package app.service;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.dao.ItemDAO;
import app.data.*;
import app.database.DatabaseConnection;
import app.enums.AuctionStatus;
import app.exception.DatabaseException;
import app.exception.ServiceException;
import app.models.Auction;
import app.models.BidTransaction;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionService {
  private record CacheSnapshot(List<Auction> data, long timestamp) {}

  private volatile CacheSnapshot snapshot = new CacheSnapshot(List.of(), 0L);
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private ItemDAO itemDAO;
  private Logger logger = LoggerFactory.getLogger(AuctionService.class);

  public AuctionService(AuctionDAO auctionDAO, BidDAO bidDAO, ItemDAO itemDAO) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
    this.itemDAO = itemDAO;
  }

  // Trả Auction thẳng — đã throw nếu không tìm thấy
  public Auction getAuctionById(int id) {
    return getAuctionById(id, "Không tìm thấy phiên ID: " + id);
  }

  public Auction createAuction(Auction auction) {
    if (auction.getEndTime().isBefore(LocalDateTime.now())) {
      throw new ServiceException("Thời gian kết thúc không thể ở quá khứ.");
    }
    return runInTransaction(
        (java.util.function.Function<Connection, Auction>) conn -> auctionDAO.save(conn, auction));
  }

  public Auction createAndStartAuction(int itemId, int sellerId, long startingPrice, int minutes) {
    return runInTransaction(
        conn -> {
          Auction auction =
              new Auction(
                  itemId, sellerId, LocalDateTime.now().plusMinutes(minutes), startingPrice);
          Auction saved = auctionDAO.save(conn, auction);
          saved.start();
          auctionDAO.update(conn, saved);
          return saved;
        });
  }

  public List<Auction> getAllAuctions() {
    CacheSnapshot current = snapshot;
    if (System.currentTimeMillis() - current.timestamp() < 2000) {
      return current.data();
    }
    List<Auction> fresh = auctionDAO.findAll();
    snapshot = new CacheSnapshot(List.copyOf(fresh), System.currentTimeMillis());
    return fresh;
  }

  public List<AuctionSummary> getAuctionSummaries() {
    List<AuctionSummary> summaries = new java.util.ArrayList<>();
    for (Auction auction : auctionDAO.findAll()) {
      AuctionSummary summary = buildSummary(auction);
      if (summary != null) {
        summaries.add(summary);
      }
    }
    return summaries;
  }

  public List<AuctionSummary> getHistorySummaries(int userId) {
    List<AuctionSummary> summaries = new java.util.ArrayList<>();
    for (Auction auction : auctionDAO.findAll()) {
      boolean isSeller = auction.getSellerId() == userId;
      boolean hasBid = bidDAO.existsBySessionAndUser(auction.getId(), userId);
      if (!isSeller && !hasBid) {
        continue;
      }
      AuctionSummary summary = buildSummary(auction);
      if (summary != null) {
        summaries.add(summary);
      }
    }
    return summaries;
  }

  public AuctionDetail getAuctionDetail(int auctionId) {
    Auction auction =
        auctionDAO
            .findById(auctionId)
            .orElseThrow(() -> new ServiceException("Không tìm thấy phiên"));
    app.models.Item item =
        itemDAO
            .findById(auction.getItemId())
            .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm"));
    long currentPrice = item.getStartingPrice();
    java.util.Optional<app.models.BidTransaction> highest = bidDAO.findHighestBid(auction.getId());
    if (highest.isPresent()) {
      currentPrice = highest.get().getAmount();
    }
    return new AuctionDetail(
        auction.getId(),
        item.getName(),
        item.getDescription(),
        item.getStartingPrice(),
        item.getStepPrice(),
        currentPrice,
        auction.getEndTime());
  }

  public AuctionResultResponse getAuctionResult(int auctionId) {
    handleCompletion(auctionId);
    Optional<BidTransaction> highest = bidDAO.findHighestBid(auctionId);
    long price = 0;
    String winnerName = "chưa có người thắng";
    int winnerId = 0;
    if (highest.isPresent()) {
      winnerName = highest.get().getBidderName();
      winnerId = highest.get().getBidderId();
      price = highest.get().getAmount();
    }
    ProfileData winner = new ProfileData(winnerId, winnerName);
    return new AuctionResultResponse(true, auctionId, winner, price);
  }

  public void handleCompletion(int auctionId) {
    runInTransaction(
        conn -> {
          auctionDAO.lockRow(conn, auctionId);
          Auction auction =
              auctionDAO
                  .findById(conn, auctionId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
          if (!auction.isExpired()) return;
          AuctionStatus s = auction.getStatus();
          if (s != AuctionStatus.OPEN && s != AuctionStatus.RUNNING) return;
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

  public void updateStatus(int auctionId, AuctionStatus status) {
    runInTransaction(
        conn -> {
          auctionDAO.lockRow(conn, auctionId);
          Auction auction =
              auctionDAO
                  .findById(conn, auctionId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
          auction.setStatus(status);
          boolean ok = auctionDAO.update(conn, auction);
          if (!ok) {
            throw new ServiceException("Không thể cập nhật trạng thái phiên: " + auctionId);
          }
        });
  }

  public void setStartTime(int auctionId, LocalDateTime startTime) {
    runInTransaction(
        conn -> {
          auctionDAO.lockRow(conn, auctionId);
          Auction auction =
              auctionDAO
                  .findById(conn, auctionId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
          auction.setStartTime(startTime);
          auctionDAO.update(conn, auction);
        });
  }

  public void setEndTime(int auctionId, LocalDateTime endTime) {
    runInTransaction(
        conn -> {
          auctionDAO.lockRow(conn, auctionId);
          Auction auction =
              auctionDAO
                  .findById(conn, auctionId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));
          auction.setEndTime(endTime);
          auctionDAO.update(conn, auction);
        });
  }

  private Auction getAuctionById(int id, String errorMessage) {
    return auctionDAO.findById(id).orElseThrow(() -> new ServiceException(errorMessage));
  }

  private AuctionSummary buildSummary(Auction auction) {
    java.util.Optional<app.models.Item> itemOpt = itemDAO.findById(auction.getItemId());
    if (itemOpt.isEmpty()) {
      return null;
    }
    app.models.Item item = itemOpt.get();
    long currentPrice = item.getStartingPrice();
    java.util.Optional<app.models.BidTransaction> highest = bidDAO.findHighestBid(auction.getId());
    if (highest.isPresent()) {
      currentPrice = highest.get().getAmount();
    }
    return new AuctionSummary(auction, item.getName(), currentPrice);
  }

  private <T> T runInTransaction(Function<Connection, T> work) {
    try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
      conn.setAutoCommit(false);
      try {
        T result = work.apply(conn);
        conn.commit();
        return result;
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi transaction.", e);
    }
  }

  // Dùng khi không cần trả về (INSERT, UPDATE, DELETE thuần)
  private void runInTransaction(Consumer<Connection> work) {
    runInTransaction(
        conn -> {
          work.accept(conn);
          return null;
        });
  }
}
