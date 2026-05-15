package app.dto;

import java.util.List;

/** HistoryResponse. */
public record HistoryResponse(List<AuctionSummary> auctions) implements Response {}
