package app.data;

/** RegisterResponse. */
public record RegisterResponse(boolean success, String message, UserData user)
    implements Response {}
