package app.data;

import app.models.BidTransaction;

public record BidRequest(int auctionId, BidTransaction bidTransaction) implements Request {}
