package app.common.dto;

import app.common.enums.UserRole;

/** Public user projection for transport payloads. */
public record UserPreview(int userId, String name, String username, UserRole role) {}
