package app.services;

import app.dao.AuctionSessionDAO;
import app.models.AuctionSession;
import app.models.AuctionStatus;
import java.util.List;

public class AuctionSessionService {
  private final AuctionSessionDAO auctionSessionDAO;

  public AuctionSessionService() {
    this.auctionSessionDAO = new AuctionSessionDAO();
  }

  public AuctionSessionService(AuctionSessionDAO dao) {
    this.auctionSessionDAO = dao;
  }

  public AuctionSession createAuctionSession(AuctionSession session) {
    return auctionSessionDAO.addAuctionSession(session);
  }

  public AuctionSession getAuctionSessionById(int sessionId) {
    return auctionSessionDAO.getAuctionSessionById(sessionId);
  }

  public boolean updateSessionStatus(int sessionId, AuctionStatus status) {
    return auctionSessionDAO.updateAuctionSessionStatus(sessionId, status);
  }

  public List<AuctionSession> getAllAuctionSessions() {
    return auctionSessionDAO.getAllAuctionSessions();
  }

  public boolean closeSessionIfExpired(int sessionId) {
    AuctionSession session = getAuctionSessionById(sessionId);
    if (session != null && session.getStatus() == AuctionStatus.ACTIVE) {
      if (java.time.LocalDateTime.now().isAfter(session.getEndTime())) {
        return updateSessionStatus(sessionId, AuctionStatus.COMPLETED);
      }
    }
    return false;
  }

  public void handleSessionCompletion(int sessionId, BidService bidService) {
    if (closeSessionIfExpired(sessionId)) {
      app.models.Bid highestBid = bidService.getHighestBid(sessionId);
      if (highestBid != null) {
        System.out.println(
            "-> Phien "
                + sessionId
                + " da ket thuc. Winner: "
                + highestBid.getBidder().getName()
                + ", Gia: $"
                + highestBid.getAmount());
      } else {
        System.out.println("-> Phien " + sessionId + " da ket thuc. Khong co ai dat gia.");
      }
    }
  }
}
