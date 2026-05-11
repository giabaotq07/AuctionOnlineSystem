package app.data;

import app.models.BidTransaction;

public record BidRequest(int sessionId, BidTransaction bidTransaction) implements Request {}
