package app.server.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PasswordUtilsTest {

  @Test
  public void testHashPassword() {
    String password = "my_secure_password";
    String hashedPassword = PasswordUtils.hashPassword(password);

    assertNotNull(hashedPassword);
    assertNotEquals(password, hashedPassword);
    assertFalse(hashedPassword.isBlank());

    // Test that the same password hashes to the same value (deterministic)
    String hashedAgain = PasswordUtils.hashPassword(password);
    assertEquals(hashedPassword, hashedAgain);
  }

  @Test
  public void testVerifySuccess() {
    String password = "my_secure_password";
    String hashedPassword = PasswordUtils.hashPassword(password);

    assertTrue(PasswordUtils.verify(password, hashedPassword));
  }

  @Test
  public void testVerifyFailure() {
    String password = "my_secure_password";
    String hashedPassword = PasswordUtils.hashPassword(password);

    assertFalse(PasswordUtils.verify("wrong_password", hashedPassword));
  }
}
