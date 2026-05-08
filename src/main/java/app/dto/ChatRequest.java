package app.dto;

import app.models.User;
import java.time.LocalDateTime;

public record ChatRequest(User sender, String content, LocalDateTime timestamp) {}
