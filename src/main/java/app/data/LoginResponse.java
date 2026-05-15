package app.data;

/** LoginResponse. */
public record LoginResponse(boolean success, String message, UserData user) implements Response {}
