package Server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseConnection {
  public static Connection getConnection() throws SQLException {
    String url = AppConfig.get("db.url") + AppConfig.get("db.name");
    return DriverManager.getConnection(url, AppConfig.get("db.user"), AppConfig.get("db.password"));
  }

  public void loadUsers() {
    String query = "SELECT * FROM users";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(query);
        ResultSet rs = pstmt.executeQuery()) {

      while (rs.next()) {
        System.out.println("User: " + rs.getString("username"));
        System.out.println("Password: " + rs.getString("password"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  //   public void loadItems() {
  //     String query = "SELECT * FROM items";
  //     try (Connection conn = DatabaseConnection.getConnection();
  //         PreparedStatement pstmt = conn.prepareStatement(query);
  //         ResultSet rs = pstmt.executeQuery()) {

  //       while (rs.next()) {
  //         System.out.println(
  //             "Item: "
  //                 + rs.getString("item_name")
  //                 + ", Current Price: "
  //                 + rs.getDouble("current_price"));
  //       }
  //     } catch (SQLException e) {
  //       e.printStackTrace();
  //     }
  //   }

  public static void main(String[] args) {
    DatabaseInitializer.initDatabase();
    DatabaseConnection db = new DatabaseConnection();
    db.loadUsers();
  }
}
