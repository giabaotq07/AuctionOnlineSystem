package app.server.dao.impl;

import app.models.BidTransaction;
import app.server.config.DatabaseConnection;
import app.server.dao.BidDAO;
import app.server.exception.DaoException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MySqlBidDAO implements BidDAO {
  private final DatabaseConnection connection = DatabaseConnection.getInstance();

  @Override
  public int create(BidTransaction bid) {
    String sql = "INSERT INTO bids(auction_id, bidder_id, amount, bid_time) VALUES(?,?,?,?)";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setInt(1, bid.getAuctionId());
      stmt.setInt(2, bid.getBidderId());
      stmt.setDouble(3, bid.getAmount());
      stmt.setTimestamp(4, Timestamp.valueOf(bid.getBidTime()));
      stmt.executeUpdate();
      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
      return 0;
    } catch (SQLException e) {
      throw new DaoException("Failed to create bid", e);
    }
  }

  @Override
  public List<BidTransaction> findByAuction(int auctionId) {
    String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_time ASC";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      try (ResultSet rs = stmt.executeQuery()) {
        List<BidTransaction> bids = new ArrayList<>();
        while (rs.next()) {
          bids.add(mapBid(rs));
        }
        return bids;
      }
    } catch (SQLException e) {
      throw new DaoException("Failed to query bids", e);
    }
  }

  @Override
  public BidTransaction findHighest(int auctionId) {
    String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY amount DESC LIMIT 1";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, auctionId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapBid(rs);
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DaoException("Failed to query highest bid", e);
    }
  }

  private BidTransaction mapBid(ResultSet rs) throws SQLException {
    int id = rs.getInt("id");
    int auctionId = rs.getInt("auction_id");
    int bidderId = rs.getInt("bidder_id");
    double amount = rs.getDouble("amount");
    LocalDateTime bidTime = rs.getTimestamp("bid_time").toLocalDateTime();
    return new BidTransaction(id, auctionId, bidderId, amount, bidTime);
  }
}

