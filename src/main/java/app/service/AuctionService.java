package app.service;

import app.config.DatabaseConnection;
import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.enums.AuctionStatus;
import app.exception.DatabaseException;
import app.exception.ServiceException;
import app.models.Auction;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionService {
  private record CacheSnapshot(List<Auction> data, long timestamp) {}

  private volatile CacheSnapshot snapshot = new CacheSnapshot(List.of(), 0L);
  private AuctionDAO auctionDAO;
  private BidDAO bidDAO;
  private Logger logger = LoggerFactory.getLogger(AuctionService.class);

  public AuctionService(AuctionDAO auctionDAO, BidDAO bidDAO) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
  }

  // Trả Auction thẳng — đã throw nếu không tìm thấy
  public Auction getAuctionById(int id) {
    return getAuctionById(id, "Không tìm thấy phiên ID: " + id);
  }

  public Auction createAuction(Auction auction) {
    if (auction.getEndTime().isBefore(LocalDateTime.now())) {
      throw new ServiceException("Thời gian kết thúc không thể ở quá khứ.");
    }
    return auctionDAO.save(auction);
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

  public void handleCompletion(int auctionId) {
    runInTransaction(
        conn -> {
          auctionDAO.lockSession(conn, auctionId);

          Auction auction =
              auctionDAO
                  .findById(conn, auctionId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));

          if (!auction.isExpired()) return;

          AuctionStatus s = auction.getStatus();
          if (s != AuctionStatus.OPEN && s != AuctionStatus.RUNNING) return;

          auctionDAO.updateStatus(conn, auctionId, AuctionStatus.FINISHED);

          bidDAO
              .findHighestBid(conn, auctionId)
              .ifPresentOrElse(
                  bid -> {
                    auctionDAO.updateWinner(conn, auctionId, bid.getBidderId());
                    logger.info(
                        "Phiên {} kết thúc. Winner: {}, Giá: {}",
                        auctionId,
                        bid.getBidderName(),
                        bid.getAmount());
                  },
                  () -> logger.info("Phiên {} kết thúc. Không có bid.", auctionId));
        });
  }

  public void updateStatus(int auctionId, AuctionStatus status) {
    boolean ok = auctionDAO.updateStatus(auctionId, status);
    if (!ok) throw new ServiceException("Không thể cập nhật trạng thái phiên: " + auctionId);
  }

  public void setStartTime(int auctionId, LocalDateTime startTime) {
    auctionDAO.updateStartTime(auctionId, startTime);
  }

  public void setEndTime(int auctionId, LocalDateTime endTime) {
    auctionDAO.updateEndTime(auctionId, endTime);
  }

  private Auction getAuctionById(int id, String errorMessage) {
    return auctionDAO.findById(id).orElseThrow(() -> new ServiceException(errorMessage));
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
