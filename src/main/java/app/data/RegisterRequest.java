package app.data;

import app.enums.UserRole;
import java.io.Serializable;

public record RegisterRequest(String name, String account, String password, UserRole role)
    implements Serializable {}
