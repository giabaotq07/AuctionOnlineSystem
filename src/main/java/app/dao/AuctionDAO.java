package app.dao;

import app.enums.AuctionStatus;
import app.models.Auction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionDAO {
  Optional<Auction> findById(int id);

  List<Auction> findAll();

  List<Auction> findByStatus(AuctionStatus status);

  List<Auction> findBySeller(int sellerId);

  Auction save(Auction auction);

  boolean updateStatus(int auctionId, AuctionStatus status);

  void updateStartTime(int auctionId, LocalDateTime startTime);

  void updateEndTime(int auctionId, LocalDateTime endTime);

  void updateWinner(int auctionId, int winnerId);

  void updateHighestBid(int sessionId, long highestBid);

  long getHighestBid(int sessionId);

  void lockSession(int sessionId);

  void extendEndTime(int sessionId, int extraSeconds);

  boolean delete(int id);
}
