package app.dto;

import java.util.List;

/** AuctionsResponse. */
public record AuctionsResponse(List<AuctionSummary> auctions) implements Response {}
