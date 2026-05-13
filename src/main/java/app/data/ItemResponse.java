package app.data;

public record ItemResponse(boolean success, String message, ItemData item) implements Response {}
