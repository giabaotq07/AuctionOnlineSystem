package app.server.dao.impl;

import app.enums.AuctionStatus;
import app.models.Auction;
import app.models.Item;
import app.server.config.connection;
import app.server.dao.AuctionDAO;
import app.server.exception.DaoException;
import app.server.utils.ItemMapper;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlAuctionDAO implements AuctionDAO {
  private final connection connection = connection.getInstance();

  @Override
  public int create(Auction auction) {
    String itemSql =
        "INSERT INTO items(name, description, start_price, step_price, category, brand, condition, artist, year, vin, vehicle_year) "
            + "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
    String auctionSql =
        "INSERT INTO auctions(item_id, seller_id, status, start_time, end_time) VALUES(?,?,?,?,?)";
    try (Connection conn = connection.getConnection()) {
      conn.setAutoCommit(false);
      int itemId;
      try (PreparedStatement itemStmt = conn.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS)) {
        fillItem(itemStmt, auction.getItem());
        itemStmt.executeUpdate();
        try (ResultSet rs = itemStmt.getGeneratedKeys()) {
          rs.next();
          itemId = rs.getInt(1);
        }
      }
      try (PreparedStatement auctionStmt = conn.prepareStatement(auctionSql, Statement.RETURN_GENERATED_KEYS)) {
        auctionStmt.setInt(1, itemId);
        auctionStmt.setInt(2, auction.getSellerId());
        auctionStmt.setString(3, auction.getStatus().name());
        auctionStmt.setTimestamp(4, Timestamp.valueOf(auction.getStartTime()));
        auctionStmt.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
        auctionStmt.executeUpdate();
        try (ResultSet rs = auctionStmt.getGeneratedKeys()) {
          rs.next();
          int auctionId = rs.getInt(1);
          conn.commit();
          return auctionId;
        }
      }
    } catch (SQLException e) {
      throw new DaoException("Failed to create auction", e);
    }
  }

  @Override
  public Optional<Auction> findById(int id) {
    String sql =
        "SELECT a.id, a.seller_id, a.status, a.start_time, a.end_time, i.* "
            + "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.id = ?";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapAuction(rs));
        }
      }
      return Optional.empty();
    } catch (SQLException e) {
      throw new DaoException("Failed to find auction", e);
    }
  }

  @Override
  public List<Auction> findAll() {
    String sql =
        "SELECT a.id, a.seller_id, a.status, a.start_time, a.end_time, i.* "
            + "FROM auctions a JOIN items i ON a.item_id = i.id";
    return queryAuctions(sql);
  }

  @Override
  public List<Auction> findLive() {
    String sql =
        "SELECT a.id, a.seller_id, a.status, a.start_time, a.end_time, i.* "
            + "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status IN ('OPEN','RUNNING')";
    return queryAuctions(sql);
  }

  @Override
  public void updateStatus(int auctionId, String status) {
    String sql = "UPDATE auctions SET status = ? WHERE id = ?";
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, status);
      stmt.setInt(2, auctionId);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new DaoException("Failed to update auction status", e);
    }
  }

  private List<Auction> queryAuctions(String sql) {
    try (Connection conn = connection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      List<Auction> auctions = new ArrayList<>();
      while (rs.next()) {
        auctions.add(mapAuction(rs));
      }
      return auctions;
    } catch (SQLException e) {
      throw new DaoException("Failed to query auctions", e);
    }
  }

  private Auction mapAuction(ResultSet rs) throws SQLException {
    int auctionId = rs.getInt("id");
    int sellerId = rs.getInt("seller_id");
    AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
    LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
    LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
    Item item = ItemMapper.map(rs);
    Auction auction = new Auction(auctionId, item, sellerId, start, end);
    auction.setStatus(status);
    return auction;
  }

  private void fillItem(PreparedStatement stmt, Item item) throws SQLException {
    stmt.setString(1, item.getName());
    stmt.setString(2, item.getDescription());
    stmt.setDouble(3, item.getStartPrice());
    stmt.setDouble(4, item.getStepPrice());
    stmt.setString(5, item.getCategory().name());
    String brand = null;
    String condition = null;
    String artist = null;
    Integer year = null;
    String vin = null;
    Integer vehicleYear = null;
    if (item instanceof app.models.Electronics e) {
      brand = e.getBrand();
      condition = e.getCondition();
    } else if (item instanceof app.models.Art a) {
      artist = a.getArtist();
      year = a.getYear();
    } else if (item instanceof app.models.Vehicle v) {
      vin = v.getVin();
      vehicleYear = v.getYear();
    }
    stmt.setString(6, brand);
    stmt.setString(7, condition);
    stmt.setString(8, artist);
    stmt.setObject(9, year);
    stmt.setString(10, vin);
    stmt.setObject(11, vehicleYear);
  }
}
