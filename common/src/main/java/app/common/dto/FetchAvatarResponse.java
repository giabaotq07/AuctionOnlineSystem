package app.common.dto;

/** FetchAvatarResponse. */
public record FetchAvatarResponse(int userId, String base64Data) implements Response {}
