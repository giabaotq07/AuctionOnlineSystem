package app.dto;

/** ItemResponse. */
public record ItemResponse(boolean success, String message, ItemData item) implements Response {}
