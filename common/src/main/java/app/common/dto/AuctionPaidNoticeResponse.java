package app.common.dto;

import java.math.BigDecimal;

/** AuctionPaidNoticeResponse. */
public record AuctionPaidNoticeResponse(
    int auctionId,
    String auctionName,
    BigDecimal amount,
    String role) implements Response {}

