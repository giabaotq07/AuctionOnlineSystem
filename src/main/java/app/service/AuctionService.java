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

public class AuctionService {
  private final AuctionDAO auctionDAO;
  private final BidDAO bidDAO; // inject thẳng, bỏ BidService param

  public AuctionService(AuctionDAO auctionDAO, BidDAO bidDAO) {
    this.auctionDAO = auctionDAO;
    this.bidDAO = bidDAO;
  }

  // Trả Auction thẳng — đã throw nếu không tìm thấy
  public Auction getAuctionById(int id) {
    return auctionDAO
        .findById(id)
        .orElseThrow(() -> new ServiceException("Không tìm thấy phiên ID: " + id));
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
    Auction auction = getAuctionById(auctionId);
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
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      bidDAO
          .findHighestBid(conn, auctionId)
          .ifPresentOrElse(
              bid -> {
                auctionDAO.updateWinner(auctionId, bid.getBidderId());
                System.out.printf(
                    "Phiên %d kết thúc. Winner: %s, Giá: %,d%n",
                    auctionId, bid.getBidderName(), bid.getAmount());
              },
              () -> System.out.println("Phiên " + auctionId + " kết thúc. Không có bid."));
    } catch (SQLException e) {
      throw new DatabaseException("Lỗi khi xử lý kết thúc phiên.", e);
    }
  }

  public void updateStatus(int auctionId, AuctionStatus status) {
    boolean ok = auctionDAO.updateStatus(auctionId, status);
    if (!ok) throw new ServiceException("Không thể cập nhật trạng thái phiên: " + auctionId);
  }
}
