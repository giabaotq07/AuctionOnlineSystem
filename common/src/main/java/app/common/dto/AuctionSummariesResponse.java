package app.common.dto;

import java.util.List;

/** AuctionSummariesResponse. */
public record AuctionSummariesResponse(List<AuctionPreview> auctions) implements Response {}
