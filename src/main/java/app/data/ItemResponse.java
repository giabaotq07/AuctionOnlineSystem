package app.data;

/** ItemResponse. */
public record ItemResponse(boolean success, String message, ItemData item) implements Response {}
