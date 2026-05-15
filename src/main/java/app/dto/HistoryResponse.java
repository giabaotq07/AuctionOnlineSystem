package app.dto;

import java.util.List;

/** HistoryResponse. */
public record HistoryResponse(boolean success, String message, List<AuctionSummary> auctions)
    implements Response {}
