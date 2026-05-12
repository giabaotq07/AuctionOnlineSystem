package app.data;

import app.models.BidTransaction;

public record BidResponse(int auctionId, BidTransaction bidTransaction) implements Response {}
