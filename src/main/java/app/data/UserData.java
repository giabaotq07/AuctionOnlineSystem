package app.data;

import app.enums.UserRole;
import app.models.User;
import java.io.Serializable;

public record UserData(int id, String name, String username, long assets, UserRole role)
    implements Serializable {
  public UserData(User user) {
    this(
        user.getId(),
        user.getName(),
        user.getAccount().getUsername(),
        user.getWallet().getAssets(),
        user.getRole());
  }
}
