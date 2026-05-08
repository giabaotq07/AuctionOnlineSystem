package app.dto;

import java.time.LocalDateTime;

public record ChatResponse(String sender, String content, LocalDateTime timestamp) {}
