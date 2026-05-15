package app.data;

import java.math.BigDecimal;

/** DepositRequest. */
public record DepositRequest(BigDecimal amount) implements Request {}
