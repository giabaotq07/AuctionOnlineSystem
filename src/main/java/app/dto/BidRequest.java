package app.dto;

import app.models.BidTransaction;

/** BidRequest. */
public record BidRequest(int auctionId, BidTransaction bidTransaction) implements Request {}
