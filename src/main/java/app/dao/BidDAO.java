package app.dao;

import app.models.BidTransaction;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface BidDAO {
  void insertBid(int auctionId, int userId, long bidAmount, boolean isAutoBid);

  void insertBid(Connection conn, int auctionId, int userId, long bidAmount, boolean isAutoBid);

  Optional<BidTransaction> findHighestBid(int auctionId);

  Optional<BidTransaction> findHighestBid(Connection conn, int auctionId);

  List<BidTransaction> findByAuction(int auctionId);

  List<BidTransaction> findByAuction(Connection conn, int auctionId);

  List<BidTransaction> findByAuctionOrderByTime(int auctionId);

  boolean existsByAuctionAndUser(int auctionId, int userId);
}
