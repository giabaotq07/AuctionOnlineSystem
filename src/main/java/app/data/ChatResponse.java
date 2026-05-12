package app.data;

import java.time.LocalDateTime;

public record ChatResponse(int senderId, String sender, String content, LocalDateTime timestamp)
    implements Response {}
