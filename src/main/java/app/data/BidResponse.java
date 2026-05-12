package app.data;

import app.models.BidTransaction;

public record BidResponse(int sessionId, BidTransaction bidTransaction) implements Response {}
