package app.data;

import app.models.Auction;

public record CreateAuctionResponse(boolean success, String message, Auction auction) {}
