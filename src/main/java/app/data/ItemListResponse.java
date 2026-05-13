package app.data;

import java.util.List;

public record ItemListResponse(boolean success, String message, List<ItemData> items)
    implements Response {}
