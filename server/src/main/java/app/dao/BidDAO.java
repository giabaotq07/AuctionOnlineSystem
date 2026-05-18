package app.dao;

import app.models.BidTransaction;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** BidDAO. */
public interface BidDAO {
  /** insertBid. */
  void insertBid(int auctionId, int userId, long bidAmount, boolean isAutoBid);

  /** insertBid. */
  void insertBid(Connection conn, int auctionId, int userId, long bidAmount, boolean isAutoBid);

  /** findHighestBid. */
  Optional<BidTransaction> findHighestBid(int auctionId);

  /** findHighestBid. */
  Optional<BidTransaction> findHighestBid(Connection conn, int auctionId);

  /** findByAuction. */
  List<BidTransaction> findByAuction(int auctionId);

  /** findByAuction. */
  List<BidTransaction> findByAuction(Connection conn, int auctionId);

  /** findByAuctionOrderByTime. */
  List<BidTransaction> findByAuctionOrderByTime(int auctionId);

  /** existsByAuctionAndUser. */
  boolean existsByAuctionAndUser(int auctionId, int userId);
}
