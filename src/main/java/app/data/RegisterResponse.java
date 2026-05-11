package app.data;

import java.io.Serializable;

public record RegisterResponse(boolean success, String message, UserData user)
    implements Serializable {}
