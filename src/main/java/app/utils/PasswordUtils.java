package app.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordUtils {
  // Hàm băm mật khẩu
  public static String hashPassword(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = digest.digest(password.getBytes());
      return Base64.getEncoder().encodeToString(encodedHash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Lỗi thuật toán mã hóa", e);
    }
  }

  public static boolean verify(String plainTextPassword, String hashedPasswordInDb) {
    String hashedInput = hashPassword(plainTextPassword);
    return hashedInput.equals(hashedPasswordInDb);
  }
}
