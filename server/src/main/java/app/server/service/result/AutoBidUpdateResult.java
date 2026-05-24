package app.server.service.result;

import app.common.models.Auction;
import app.common.models.AutoBid;
import app.common.models.User;

/** AutoBidUpdateResult. */
public record AutoBidUpdateResult(Auction auction, AutoBid autoBid, User user) {}
