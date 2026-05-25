package app.common.dto;

public record BanUserResponse(int userId, boolean isBanned) implements Response {}
