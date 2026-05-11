package app.data;

import java.io.Serializable;

public record AuctionResultResponse(
    boolean success, String message, String winnerName, long finalPrice) implements Serializable {}
