package app.service;

import app.dao.AuctionSessionDAO;
import app.enums.AuctionStatus;
import app.exceptions.ServiceException;
import app.models.Auction;
import app.models.Bid;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionSessionService {
  private final AuctionSessionDAO auctionSessionDAO;

  public AuctionSessionService(AuctionSessionDAO auctionSessionDAO) {
    this.auctionSessionDAO = auctionSessionDAO;
  }

  public Auction createAuctionSession(Auction session) {
    // C thể thm logic kiểm tra: thời gian kết thc phải ở tương lai
    if (session.getEndTime().isBefore(LocalDateTime.now())) {
      throw new ServiceException("Thời gian kết thc khng thể ở qu khứ.");
    }
    return auctionSessionDAO.addAuctionSession(session);
  }

  public Auction getAuctionSessionById(int sessionId) {
    Auction session = auctionSessionDAO.getAuctionSessionById(sessionId);
    if (session == null) {
      throw new ServiceException("Khng tm thấy phin đấu gi với ID: " + sessionId);
    }
    return session;
  }

  public void updateSessionStatus(int sessionId, AuctionStatus status) {
    boolean ok = auctionSessionDAO.updateAuctionSessionStatus(sessionId, status);
    if (!ok) {
      throw new ServiceException("Khng thể cập nhật trạng thi cho phin ID: " + sessionId);
    }
  }

  public List<Auction> getAllAuctionSessions() {
    return auctionSessionDAO.getAllAuctionSessions();
  }

  public boolean closeSessionIfExpired(int sessionId) {
    try {
      Auction session = getAuctionSessionById(sessionId);

      if (session.getStatus() == AuctionStatus.ACTIVE
          && LocalDateTime.now().isAfter(session.getEndTime())) {

        updateSessionStatus(sessionId, AuctionStatus.COMPLETED);
        return true;
      }
    } catch (Exception e) {
      System.err.println("Lỗi khi kiểm tra hết hạn phin " + sessionId + ": " + e.getMessage());
    }
    return false;
  }

  public void handleSessionCompletion(int sessionId, BidService bidService) {
    if (closeSessionIfExpired(sessionId)) {
      Bid highestBid = bidService.getHighestBid(sessionId);

      if (highestBid != null) {
        System.out.println(
            String.format(
                "-> Phiên %d kết thúc. Winner: %s, Giá: $%.2f",
                sessionId, highestBid.getBidder().getName(), highestBid.getAmount()));

        // Tại đây bạn có thể thêm logic trừ tiền người thắng hoặc cộng tiền người bán
      } else {
        System.out.println("-> Phiên " + sessionId + " kết thúc. Không có người đặt giá.");
      }
    }
  }
}
