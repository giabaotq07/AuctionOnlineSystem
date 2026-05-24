package app.common.dto;

import java.util.List;

/** AuctionHistoryResponse. */
public record AuctionHistoryResponse(List<AuctionPreview> auctions, boolean fullSnapshot)
    implements Response {}
