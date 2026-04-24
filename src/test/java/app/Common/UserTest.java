package app.Common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserTest {
  private User user;

  @BeforeEach
  public void setUser() {
    user = new User("01", "Tester");
  }

  @Test
  public void Deposit_Positive() {
    user.Deposit(100000);
    assertEquals(100000, user.getAssets());
  }

  @Test
  public void Deposit_Negative() {
    assertThrows(IllegalArgumentException.class, () -> user.Deposit(-100000));
  }

  @Test
  public void Withdraw_Success() {
    user.Deposit(100000);
    user.Withdraw(50000);
    assertEquals(50000, user.getAssets());
  }

  @Test
  public void Withdraw_Fail() {
    assertThrows(IllegalArgumentException.class, () -> user.Withdraw(50000));
  }

  @Test
  public void Withdraw_Negative() {
    assertThrows(IllegalArgumentException.class, () -> user.Withdraw(-50000));
  }
}
