package app.data;

import app.models.Auction;

public record AuctionSummary(Auction auction, String itemName, long currentPrice) {}
