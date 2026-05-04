package app.dto;

import app.models.User;
import java.io.Serial;
import java.io.Serializable;

public record LoginResponse(boolean success, String message, User user) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
