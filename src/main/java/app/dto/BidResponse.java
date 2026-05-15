package app.dto;

import app.models.BidTransaction;

/** BidResponse. */
public record BidResponse(int auctionId, BidTransaction bidTransaction) implements Response {}
