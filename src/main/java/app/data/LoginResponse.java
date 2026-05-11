package app.data;

public record LoginResponse(boolean success, String message, UserData user) implements Response {}
