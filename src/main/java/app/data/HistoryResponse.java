package app.data;

import java.util.List;

public record HistoryResponse(boolean success, String message, List<AuctionSummary> auctions)
    implements Response {}
