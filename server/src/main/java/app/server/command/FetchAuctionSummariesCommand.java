package app.server.command;

import app.common.dto.AuctionSummariesResponse;
import app.common.enums.ResponseType;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionSummariesCommand. */
public class FetchAuctionSummariesCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionSummariesCommand.class);
  private final AuctionService auctionService;

  /** FetchAuctionSummariesCommand. */
  public FetchAuctionSummariesCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      AuctionSummariesResponse response =
          new AuctionSummariesResponse(auctionService.getAuctionSummaries());
      clientHandler.sendPacket(
          PacketRes.of(ResponseType.FETCH_AUCTION_SUMMARIES_RESULT, "OK", response));
    } catch (Exception e) {
      logger.error("Failed to fetch auctions", e);
      clientHandler.sendPacket(
          PacketRes.error(ResponseType.ERROR, "Không thể tải danh sách đấu giá"));
    }
  }
}
