package app.common.dto;

/** FetchAvatarRequest. */
public record FetchAvatarRequest(int userId, String avatarUrl) implements Request {}
