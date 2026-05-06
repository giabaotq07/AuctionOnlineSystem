package app.dao;

public interface AutoBidDAO {
  void delete(int sessionId, int userId);

  void upsert(int sessionId, int userId, long maxBid, long increment);
}
