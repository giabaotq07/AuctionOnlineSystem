package app.common.dto;

import app.common.enums.UserRole;

public record AccountDto(String username, UserRole role) {}
