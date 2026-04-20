package app.dao;

import app.config.DatabaseConnection;
import app.config.PasswordUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    // Trả về true nếu đăng nhập thành công
    public boolean checkLogin(String account, String password) {
        String query = "SELECT id FROM users WHERE account = ? AND password = ?";

        // Dùng try-with-resources để tự động đóng kết nối (rất hợp với JDK bản mới)
        try (Connection conn = DatabaseConnection.getConnection();  PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, account);
            pstmt.setString(2,PasswordUtils.hashPassword(password)); // Ở hệ thống thực tế, bạn sẽ phải hash mật khẩu trước khi so sánh

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Nếu ResultSet có dữ liệu -> Sai/Đúng
            }

        } catch (SQLException e) {
            return false;
        }
    }

    public void loadUsers() {
        String query = "SELECT * FROM users";
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println("User: " + rs.getString("username"));
                System.out.println("Password: " + rs.getString("password"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int addUser(String account, String password, String name) {
        String insertSql = "INSERT INTO users (account, password, name) VALUES (?, ?, ?)";
        int generatedId = -1;

        // 1. Chú ý tham số thứ 2: Statement.RETURN_GENERATED_KEYS
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, account);
            pstmt.setString(2, PasswordUtils.hashPassword(password)); // Nhớ hash mật khẩu nhé!
            pstmt.setString(3, name);

            // 2. Chạy lệnh INSERT
            int rowsAffected = pstmt.executeUpdate();

            // 3. Nếu INSERT thành công (ảnh hưởng > 0 dòng)
            if (rowsAffected > 0) {
                // Lấy ra danh sách các khóa (ID) vừa được tạo
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1); // Cột 1 chính là ID tự tăng
                        System.out.println("Đăng ký thành công! ID tự động của user là: " + generatedId);
                    }
                }
            }
        } catch (SQLException e) {
            // Mã 1062 là Duplicate Entry (Trùng lặp khóa chính hoặc cột UNIQUE)
            if (e.getErrorCode() == 1062) {
                System.out.println("Tài khoản '" + account + "' đã có người sử dụng!");
            } else {
                e.printStackTrace(); // In ra lỗi khác nếu có
            }
        }
        return generatedId;
    }

    public void deleteUser(String account) {
        String deleteSql = "DELETE FROM users WHERE account = ?";
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(deleteSql);
            pstmt.setString(1, account);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User deleted: " + account);
            } else {
                System.out.println("User '" + account + "' không tồn tại.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
