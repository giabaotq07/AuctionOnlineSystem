package app.dao;

import app.enums.AuctionStatus;
import app.models.Auction;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Auction operations.
 *
 * <p>Methods are categorized as: 1. Read-only operations: Create own connection (no Connection
 * param) 2. Transaction operations: Accept Connection parameter for participation in transactions
 */
public interface AuctionDAO {
  // ── Read operations (create own connection) ──────────────────────
  Optional<Auction> findById(int id);

  Optional<Auction> findById(Connection conn, int id);

  List<Auction> findAll();

  List<Auction> findByStatus(AuctionStatus status);

  List<Auction> findBySeller(int sellerId);

  long getHighestBid(int sessionId);

  // ── Write operations for single objects ──────────────────────────
  Auction save(Auction auction);

  boolean updateStatus(int auctionId, AuctionStatus status);

  void updateStartTime(int auctionId, LocalDateTime startTime);

  void updateEndTime(int auctionId, LocalDateTime endTime);

  void updateEndTime(Connection conn, int auctionId, LocalDateTime endTime);

  void updateWinner(int auctionId, int winnerId);

  boolean delete(int id);

  // ── Transaction-aware operations (use provided Connection) ────────
  // These are used within transactions to ensure ACID properties

  void lockSession(Connection conn, int sessionId);

  long getHighestBid(Connection conn, int sessionId);

  void updateHighestBid(Connection conn, int sessionId, long highestBid);

  void extendEndTime(Connection conn, int sessionId, int extraSeconds);

  Auction save(Connection conn, Auction auction);

  boolean updateStatus(Connection conn, int auctionId, AuctionStatus status);

  void updateWinner(Connection conn, int auctionId, int winnerId);
}
