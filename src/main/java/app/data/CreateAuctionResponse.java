package app.data;

import app.models.Auction;
import java.io.Serializable;

public record CreateAuctionResponse(boolean success, String message, Auction auction)
    implements Serializable {}
