package app.services;

import app.dao.BidTransactionDAO;
import app.models.BidTransaction;
import java.util.List;

public class BidTransactionService {
  private final BidTransactionDAO bidTransactionDAO;

  public BidTransactionService(BidTransactionDAO bidTransactionDAO) {
    this.bidTransactionDAO = bidTransactionDAO;
  }

  public boolean createTransaction(int amount, int auctionId, int userId) {
    BidTransaction tx = new BidTransaction(amount, auctionId, userId);
    return bidTransactionDAO.addTransaction(tx);
  }

  public List<BidTransaction> getUserTransactions(int userId) {
    return bidTransactionDAO.getTransactionsByUser(userId);
  }
}
