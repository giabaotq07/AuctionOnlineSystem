package app.dao;

import app.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuctionDAO {
  // Giả sử bạn đã có phương thức lấy Connection từ Singleton DatabaseConnection
  public void placeBid(int itemId, int userId, double bidAmount) {
    String updatePriceSql =
        "UPDATE Items SET current_price = ? WHERE item_id = ? AND current_price < ?";
    String insertHistorySql =
        "INSERT INTO Bids (item_id, user_id, amount, bid_time) VALUES (?, ?, ?, NOW())";

    Connection conn = null;
    try {
      conn = DatabaseConnection.getConnection();
      // 1. Tắt chế độ tự động commit để bắt đầu Transaction
      conn.setAutoCommit(false);

      // 2. Cập nhật giá sản phẩm
      try (PreparedStatement updateStmt = conn.prepareStatement(updatePriceSql)) {
        updateStmt.setDouble(1, bidAmount);
        updateStmt.setInt(2, itemId);
        updateStmt.setDouble(3, bidAmount);
        int rowsUpdated = updateStmt.executeUpdate();

        if (rowsUpdated > 0) {
          // 3. Nếu cập nhật thành công, lưu vào lịch sử
          try (PreparedStatement insertStmt = conn.prepareStatement(insertHistorySql)) {
            insertStmt.setInt(1, itemId);
            insertStmt.setInt(2, userId);
            insertStmt.setDouble(3, bidAmount);
            insertStmt.executeUpdate();
          }
          // 4. Commit toàn bộ thay đổi
          conn.commit();
        } else {
          System.out.println("Giá đấu quá thấp hoặc sản phẩm không tồn tại!");
          conn.rollback();
        }
      }
    } catch (SQLException e) {
      try {
        if (conn != null) conn.rollback(); // Rollback nếu có lỗi bất kỳ
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
      e.printStackTrace();
    } finally {
      // Đóng kết nối
      if (conn != null) {
        try {
          conn.setAutoCommit(true);
          conn.close();
        } catch (SQLException e) {
        }
      }
    }
  }
}
