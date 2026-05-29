package app.common.dto;

/** FetchAvatarResponse. */
public record FetchAvatarResponse(int userId, String base64Data, String avatarUrl)
    implements Response {
  public FetchAvatarResponse(int userId, String base64Data) {
    this(userId, base64Data, null);
  }
}
