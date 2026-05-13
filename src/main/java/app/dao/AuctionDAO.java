package app.dao;

import app.enums.AuctionStatus;
import app.models.Auction;
import java.sql.Connection;
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

  List<Auction> findByItemId(Connection conn, int itemId);

  // ── Write operations for single objects ──────────────────────────
  Auction save(Auction auction);

  boolean update(Auction auction);

  boolean delete(int id);

  // ── Transaction-aware operations (use provided Connection) ────────
  // These are used within transactions to ensure ACID properties
  void lockRow(Connection conn, int auctionId);

  Auction save(Connection conn, Auction auction);

  boolean update(Connection conn, Auction auction);
}
