package app.data;

import app.models.Auction;
import java.io.Serializable;

public record AuctionSummary(Auction auction, String itemName, long currentPrice)
    implements Serializable {}
