package app.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserTest {
  private User user;

  @BeforeEach
  public void setUser() {
    user =
        UserFactory.createUser(
            "Tester",
            new Account("test_account", "test_password"),
            new Wallet(),
            UserRole.BIDDER.name());
  }

  @Test
  public void Deposit_Positive() {
    user.getWallet().deposit(100000);
    assertEquals(100000, user.getWallet().getAssets());
  }

  @Test
  public void Deposit_Negative() {
    assertThrows(IllegalArgumentException.class, () -> user.getWallet().deposit(-100000));
  }

  @Test
  public void Withdraw_Success() {
    user.getWallet().deposit(100000);
    user.getWallet().withdraw(50000);
    assertEquals(50000, user.getWallet().getAssets());
  }

  @Test
  public void Withdraw_Fail() {
    assertThrows(IllegalArgumentException.class, () -> user.getWallet().withdraw(50000));
  }

  @Test
  public void Withdraw_Negative() {
    assertThrows(IllegalArgumentException.class, () -> user.getWallet().withdraw(-50000));
  }
}
