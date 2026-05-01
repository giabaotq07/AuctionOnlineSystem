package app.service;

import app.dao.AuctionDAO;
import app.enums.AuctionStatus;
import app.exception.ServiceException;
import app.models.Auction;
import app.models.BidTransaction;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
  private final AuctionDAO auctionDAO;

  public AuctionService(AuctionDAO auctionDAO) {
    this.auctionDAO = auctionDAO;
  }

  public Auction createAuctionSession(Auction session) {
    if (session.getEndTime().isBefore(LocalDateTime.now())) {
      throw new ServiceException("Thời gian kết thc khng thể ở qu khứ.");
    }
    return auctionDAO.addAuction(session);
  }

  public Auction getAuctionById(int sessionId) {
    Auction session = auctionDAO.getAuctionById(sessionId);
    if (session == null) {
      throw new ServiceException("Khng tm thấy phin đấu gi với ID: " + sessionId);
    }
    return session;
  }

  public void updateSessionStatus(int sessionId, AuctionStatus status) {
    boolean ok = auctionDAO.updateAuctionStatus(sessionId, status);
    if (!ok) {
      throw new ServiceException("Khng thể cập nhật trạng thi cho phin ID: " + sessionId);
    }
  }

  public List<Auction> getAllAuction() {
    return auctionDAO.getAllAuction();
  }

  public boolean closeSessionIfExpired(int sessionId) {
    try {
      Auction session = getAuctionById(sessionId);

      if (session.getStatus() == AuctionStatus.ACTIVE
          && LocalDateTime.now().isAfter(session.getEndTime())) {

        updateSessionStatus(sessionId, AuctionStatus.COMPLETED);
        return true;
      }
    } catch (Exception e) {
      System.err.println("Lỗi khi kiểm tra hết hạn phiên " + sessionId + ": " + e.getMessage());
      e.printStackTrace();
    }
    return false;
  }

  public void handleSessionCompletion(int sessionId, BidService bidService) {
    if (closeSessionIfExpired(sessionId)) {
      BidTransaction highestBidTransaction = bidService.getHighestBid(sessionId);

      if (highestBidTransaction != null) {
        System.out.println(
            String.format(
                "-> Phiên %d kết thúc. Winner: %s, Giá: $%.2f",
                sessionId,
                highestBidTransaction.getBidder().getName(),
                highestBidTransaction.getAmount()));

        // Tại đây bạn có thể thêm logic trừ tiền người thắng hoặc cộng tiền người bán
      } else {
        System.out.println("-> Phiên " + sessionId + " kết thúc. Không có người đặt giá.");
      }
    }
  }
}
