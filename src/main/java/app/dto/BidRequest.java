package app.dto;

import app.models.BidTransaction;

public record BidRequest(int sessionId, BidTransaction  bidTransaction) {}
