package app.service;

import app.dao.BidDAO;
import app.models.BidTransaction;
import java.util.List;

public class BidService {
  private final BidDAO bidDAO;

  public BidService(BidDAO bidDAO) {
    this.bidDAO = bidDAO;
  }

  public void placeBid(int sessionId, int userId, double amount) {
    // Việc kiểm tra bid cao hơn giá hiện tại và ngăn chặn lost update
    // đã được đưa vào Transaction trong BidDAO.placeBid sử dụng Pessimistic Lock.
    bidDAO.placeBid(sessionId, userId, amount);
  }

  public List<BidTransaction> getBidsBySession(int sessionId) {
    return bidDAO.getBidsBySession(sessionId);
  }

  public BidTransaction getHighestBid(int sessionId) {
    return bidDAO.getHighestBid(sessionId);
  }
}
