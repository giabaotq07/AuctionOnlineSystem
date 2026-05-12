package app.data;

import java.math.BigDecimal;

public record DepositRequest(BigDecimal amount) implements Request {}
