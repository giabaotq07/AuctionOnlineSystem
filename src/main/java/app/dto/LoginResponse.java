package app.dto;

import app.enums.UserRole;

public record LoginResponse(boolean success, String message, int userId, UserRole role, String sessionToken) {
}
