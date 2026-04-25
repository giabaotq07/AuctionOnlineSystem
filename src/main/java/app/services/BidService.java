package app.services;

import app.dao.BidDAO;
import app.models.Bid;
import java.util.List;

public class BidService {
  private final BidDAO bidDAO;

  public BidService() {
    this.bidDAO = new BidDAO();
  }

  public BidService(BidDAO bidDAO) {
    this.bidDAO = bidDAO;
  }

  public boolean placeBid(int sessionId, int userId, double amount) {
    return bidDAO.placeBid(sessionId, userId, amount);
  }

  public List<Bid> getBidsBySession(int sessionId) {
    return bidDAO.getBidsBySession(sessionId);
  }

  public Bid getHighestBid(int sessionId) {
    List<Bid> bids = bidDAO.getBidsBySession(sessionId);
    if (bids == null || bids.isEmpty()) return null;
    Bid highest = bids.get(0);
    for (Bid b : bids) {
      if (b.getAmount() > highest.getAmount()) highest = b;
    }
    return highest;
  }
}
