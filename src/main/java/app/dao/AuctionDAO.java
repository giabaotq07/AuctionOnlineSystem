package app.dao;

import app.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuctionDAO {
  public void placeBid(int itemId, int userId, double bidAmount) {
    String insertSql =
        "INSERT INTO bids (item_id, user_id, bid_amount) VALUES (?, ?, ?)"; // Giả sử bạn có bảng
    // bids

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

      pstmt.setInt(1, itemId);
      pstmt.setInt(2, userId);
      pstmt.setDouble(3, bidAmount);

      pstmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
