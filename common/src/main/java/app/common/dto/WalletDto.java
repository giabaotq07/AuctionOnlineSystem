package app.common.dto;

import java.math.BigDecimal;
import java.util.Map;

public record WalletDto(BigDecimal availableBalance, Map<String, BigDecimal> frozenFunds) {}
