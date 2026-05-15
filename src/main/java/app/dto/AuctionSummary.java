package app.dto;

import app.models.Auction;

/** AuctionSummary. */
public record AuctionSummary(Auction auction, String itemName, long currentPrice) {}
