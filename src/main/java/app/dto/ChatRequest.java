package app.dto;

import java.time.LocalDateTime;

/** ChatRequest. */
public record ChatRequest(UserData sender, String content, LocalDateTime timestamp)
    implements Request {}
