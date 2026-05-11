package app.data;

public record RegisterResponse(boolean success, String message, UserData user)
    implements Response {}
