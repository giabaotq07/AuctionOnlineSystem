package app.dto;

import java.util.List;

/** AuctionsResponse. */
public record AuctionsResponse(boolean success, String message, List<AuctionSummary> auctions)
    implements Response {}
