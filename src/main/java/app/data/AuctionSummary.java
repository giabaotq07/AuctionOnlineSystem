package app.data;

import app.models.Auction;

/** AuctionSummary. */
public record AuctionSummary(Auction auction, String itemName, long currentPrice) {}
