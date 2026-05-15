package app.dto;

/** RegisterResponse. */
public record RegisterResponse(boolean success, String message, UserData user)
    implements Response {}
