package app.common.dto;

import java.util.List;

/** ItemListResponse. */
public record ItemListResponse(List<ItemPreview> items) implements Response {}
