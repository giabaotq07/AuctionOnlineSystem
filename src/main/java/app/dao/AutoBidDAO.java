package app.dao;

import java.sql.Connection;

public interface AutoBidDAO {
  void delete(int sessionId, int userId);

  void delete(Connection conn, int sessionId, int userId);

  void upsert(int sessionId, int userId, long maxBid, long increment);

  //  List<AutoBidConfig> findBySession(Connection conn, int sessionId);
  void upsert(Connection conn, int sessionId, int userId, long maxBid, long increment);
}
