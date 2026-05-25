package app.common.dto;

public record BanUserRequest(int userId, boolean ban) implements Request {}