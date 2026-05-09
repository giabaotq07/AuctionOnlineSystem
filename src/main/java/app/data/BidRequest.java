package app.data;

import app.models.BidTransaction;
import java.io.Serializable;

public record BidRequest(int sessionId, BidTransaction bidTransaction) implements Serializable {}
