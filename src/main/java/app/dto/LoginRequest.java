package app.dto;

import java.io.Serial;
import java.io.Serializable;

public record LoginRequest(String username, String password) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
