package app.data;

import app.models.BidTransaction;

/** BidResponse. */
public record BidResponse(int auctionId, BidTransaction bidTransaction) implements Response {}
