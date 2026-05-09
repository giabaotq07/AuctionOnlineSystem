package app.service;

import app.config.TransactionManager;
import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.enums.AuctionStatus;
import app.exception.ServiceException;
import app.models.Auction;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionService {
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO;
  private final TransactionManager txManager;
  private final Logger logger = LoggerFactory.getLogger(AuctionService.class);

  public AuctionService(AuctionDAO auctionDAO, BidDAO bidDAO) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
    txManager = TransactionManager.getInstance();
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
    return auctionDAO.findAll();
  }

  public void handleCompletion(int auctionId) {
    txManager.runInTransaction(
        conn -> {
          auctionDAO.lockSession(conn, auctionId);

          Auction auction =
              auctionDAO
                  .findById(auctionId)
                  .orElseThrow(() -> new ServiceException("Không tìm thấy phiên: " + auctionId));

          if (!auction.isExpired()) return;

          AuctionStatus s = auction.getStatus();
          if (s != AuctionStatus.OPEN && s != AuctionStatus.RUNNING) return;

          auctionDAO.updateStatus(conn, auctionId, AuctionStatus.FINISHED);

          bidDAO
              .findHighestBid(auctionId)
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
}
