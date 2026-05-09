package app.dao;

import app.enums.AuctionStatus;
import app.models.Auction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionDAO {
  Optional<Auction> findById(int id);

  List<Auction> findAll();

  List<Auction> findByStatus(AuctionStatus status);

  List<Auction> findBySeller(int sellerId);

  void extendEndTime(Connection conn, int sessionId, int extraSeconds);

  Auction save(Auction auction);

  Auction save(Connection conn, Auction auction);

  boolean updateStatus(int auctionId, AuctionStatus status);

  boolean updateStatus(Connection conn, int auctionId, AuctionStatus status);

  void updateStartTime(int auctionId, LocalDateTime startTime);

  void updateEndTime(int auctionId, LocalDateTime endTime);

  void updateWinner(int auctionId, int winnerId);

  long getHighestBid(Connection conn, int sessionId);

  void updateHighestBid(int sessionId, long highestBid);

  void lockSession(Connection conn, int sessionId);

  long getHighestBid(int sessionId);

  void lockSession(int sessionId);

  void updateHighestBid(Connection conn, int sessionId, long highestBid);

  void extendEndTime(int sessionId, int extraSeconds);

  void updateWinner(Connection conn, int auctionId, int winnerId);

  boolean delete(int id);
}
