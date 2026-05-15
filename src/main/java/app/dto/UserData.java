package app.dto;

import app.enums.UserRole;
import app.models.User;
import java.math.BigDecimal;
import java.util.Map;

/** UserData. */
public record UserData(
    int id,
    String name,
    String username,
    BigDecimal availableBalance,
    Map<String, BigDecimal> frozenFunds,
    UserRole role) {
  /** UserData. */
  public UserData(User user) {
    this(
        user.getId(),
        user.getName(),
        user.getAccount().getUsername(),
        user.getWallet().getAvailableBalance(),
        user.getWallet().getFrozenFundsSnapshot(),
        user.getRole());
  }
}
