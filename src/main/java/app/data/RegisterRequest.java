package app.data;

import app.enums.UserRole;

public record RegisterRequest(String name, String account, String password, UserRole role) {}
