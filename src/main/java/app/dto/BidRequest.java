package app.dto;

import app.models.BidTransaction;
import java.io.Serial;
import java.io.Serializable;

public record BidRequest(int sessionId, BidTransaction bidTransaction) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
