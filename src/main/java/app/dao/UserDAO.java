package app.dao;

import app.config.DatabaseConnection;
import app.config.PasswordUtils;
import app.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    // Trả về true nếu đăng nhập thành công
    public boolean checkLogin(String username, String password) {
        String query = "SELECT id FROM users WHERE username = ? AND password = ?";

        // Dùng try-with-resources để tự động đóng kết nối (rất hợp với JDK bản mới)
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, PasswordUtils.hashPassword(password)); // Ở hệ thống thực tế, bạn sẽ phải hash mật khẩu trước khi so sánh

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Nếu ResultSet có dữ liệu -> Sai/Đúng
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}