package app.dao;

import app.models.AutoBid;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface AutoBidDAO {
  // ── Read operations ───────────────────────────────
  Optional<AutoBid> findById(int id);

  Optional<AutoBid> findByAuctionAndUser(int auctionId, int userId);

  List<AutoBid> findByAuction(int auctionId);

  List<AutoBid> findEnabledByAuction(int auctionId);

  // ── Write operations ──────────────────────────────
  AutoBid save(AutoBid autoBid);

  boolean update(AutoBid autoBid);

  boolean delete(int id);

  boolean setEnabled(int id, boolean enabled);

  // ── Transaction-aware operations ──────────────────
  Optional<AutoBid> findByAuctionAndUser(Connection conn, int auctionId, int userId);

  List<AutoBid> findEnabledByAuction(Connection conn, int auctionId);

  AutoBid save(Connection conn, AutoBid autoBid);

  boolean update(Connection conn, AutoBid autoBid);
}
