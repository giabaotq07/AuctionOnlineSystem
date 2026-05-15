package app.dto;

import app.enums.UserRole;

/** RegisterRequest. */
public record RegisterRequest(String name, String account, String password, UserRole role)
    implements Request {}
