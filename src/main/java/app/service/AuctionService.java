package app.service;

import app.dao.AuctionDAO;
import app.dao.BidDAO;
import app.enums.AuctionStatus;
import app.exception.ServiceException;
import app.models.Auction;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO; // inject thẳng, bỏ BidService param

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
    return auctionDAO.findAll();
  }

  public boolean closeIfExpired(int auctionId) {
    Auction auction = getAuctionById(auctionId, "Không tìm thấy phiên ID: " + auctionId);
    auction.getLock().lock();
    try {
      if (!auction.isExpired()) return false;

      AuctionStatus s = auction.getStatus();
      if (s != AuctionStatus.OPEN && s != AuctionStatus.RUNNING) return false;

      auction.finish();
      auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
      return true;

    } finally {
      auction.getLock().unlock();
    }
  }

  public void handleCompletion(int auctionId) {
    if (!closeIfExpired(auctionId)) return;
    bidDAO
        .findHighestBid(auctionId)
        .ifPresentOrElse(
            bid -> {
              auctionDAO.updateWinner(auctionId, bid.getBidderId());
              System.out.printf(
                  "Phiên %d kết thúc. Winner: %s, Giá: %,d%n",
                  auctionId, bid.getBidderName(), bid.getAmount());
            },
            () -> System.out.println("Phiên " + auctionId + " kết thúc. Không có bid."));
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
