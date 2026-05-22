package app.common.dto;

import app.common.enums.UserRole;
import app.common.models.User;

/** Public user projection for transport payloads. */
public record UserPreview(int userId, String name, String username, UserRole role) {
  public static UserPreview from(User user) {
    if (user == null) {
      return null;
    }
    var account = user.getAccount();
    return new UserPreview(
        user.getId(),
        user.getName(),
        account == null ? null : account.getUsername(),
        account == null ? null : account.getRole());
  }
}
