package app.data;

import java.time.LocalDateTime;

public record ChatRequest(UserData sender, String content, LocalDateTime timestamp) {}
