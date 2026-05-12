package app.dao;

import app.models.BidTransaction;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface BidDAO {
  void insertBid(int sessionId, int userId, long bidAmount, boolean isAutoBid);

  void insertBid(Connection conn, int sessionId, int userId, long bidAmount, boolean isAutoBid);

  Optional<BidTransaction> findHighestBid(int sessionId);

  Optional<BidTransaction> findHighestBid(Connection conn, int sessionId);

  List<BidTransaction> findBySession(int sessionId);

  List<BidTransaction> findBySessionForChart(int sessionId);

  boolean existsBySessionAndUser(int sessionId, int userId);
}
