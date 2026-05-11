package app.data;

import java.util.List;

public record AuctionsResponse(boolean success, String message, List<AuctionSummary> auctions) {}
