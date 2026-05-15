package app.data;

import app.models.BidTransaction;

/** BidRequest. */
public record BidRequest(int auctionId, BidTransaction bidTransaction) implements Request {}
