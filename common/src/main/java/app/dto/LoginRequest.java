package app.dto;

/** LoginRequest. */
public record LoginRequest(String username, String password) implements Request {}
