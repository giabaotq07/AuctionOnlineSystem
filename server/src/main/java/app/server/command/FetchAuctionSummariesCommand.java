package app.server.command;

import app.common.dto.AuctionSummariesResponse;
import app.common.enums.ResponseType;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AuctionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionSummariesCommand. */
public class FetchAuctionSummariesCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionSummariesCommand.class);
  private final AuctionQueryService auctionQueryService;

  /** FetchAuctionSummariesCommand. */
  public FetchAuctionSummariesCommand(AuctionQueryService auctionQueryService) {
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      AuctionSummariesResponse response =
          new AuctionSummariesResponse(auctionQueryService.getAuctionPreviews());
      clientHandler.sendPacket(
          PacketRes.of(ResponseType.FETCH_AUCTION_SUMMARIES_RESULT, "OK", response));
    } catch (Exception e) {
      logger.error("Failed to fetch auctions", e);
      clientHandler.sendPacket(
          PacketRes.error(
              ResponseType.FETCH_AUCTION_SUMMARIES_RESULT, "Không thể tải danh sách đấu giá"));
    }
  }
}
