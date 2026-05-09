package app.data;

import java.io.Serializable;

public record LoginResponse(boolean success, String message, UserData user)
    implements Serializable {}
