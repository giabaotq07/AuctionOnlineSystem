package app.dao;

import app.config.DatabaseConnection;
import app.exceptions.DatabaseException;
import app.models.BidTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {

  public boolean addTransaction(BidTransaction transaction) {
    String query = "INSERT INTO bid_transactions (auction_id, user_id, amount) VALUES (?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, transaction.getAuctionId());
      pstmt.setInt(2, transaction.getUserId());
      pstmt.setInt(3, transaction.getAmount());
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new DatabaseException("Database/Service error", e);
    }
  }

  public List<BidTransaction> getTransactionsByUser(int userId) {
    List<BidTransaction> list = new ArrayList<>();
    String query = "SELECT * FROM bid_transactions WHERE user_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
      pstmt.setInt(1, userId);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          list.add(
              new BidTransaction(
                  rs.getInt("amount"), rs.getInt("auction_id"), rs.getInt("user_id")));
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Database/Service error", e);
    }
    return list;
  }
}
