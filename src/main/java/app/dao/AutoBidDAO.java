package app.dao;

import app.models.AutoBid;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface AutoBidDAO {
  // ── Read operations ───────────────────────────────
  Optional<AutoBid> findById(int id);

  Optional<AutoBid> findBySessionAndUser(int sessionId, int userId);

  List<AutoBid> findBySession(int sessionId);

  List<AutoBid> findEnabledBySession(int sessionId);

  // ── Write operations ──────────────────────────────
  AutoBid save(AutoBid autoBid);

  boolean update(AutoBid autoBid);

  boolean delete(int id);

  boolean setEnabled(int id, boolean enabled);

  // ── Transaction-aware operations ──────────────────
  Optional<AutoBid> findBySessionAndUser(Connection conn, int sessionId, int userId);

  List<AutoBid> findEnabledBySession(Connection conn, int sessionId);

  AutoBid save(Connection conn, AutoBid autoBid);

  boolean update(Connection conn, AutoBid autoBid);
}
