package app.server.command;

import app.common.dto.AuctionHistoryRequest;
import app.common.dto.AuctionHistoryResponse;
import app.common.enums.ResponseType;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AuctionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionHistoryCommand. */
public class FetchAuctionHistoryCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionHistoryCommand.class);
  private final AuctionQueryService auctionQueryService;

  /** FetchAuctionHistoryCommand. */
  public FetchAuctionHistoryCommand(AuctionQueryService auctionQueryService) {
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      int userId = clientHandler.getUser().getId();
      AuctionHistoryRequest request = packet.getData(AuctionHistoryRequest.class);
      int sinceVersion = request == null ? -1 : request.sinceVersion();
      var auctions = auctionQueryService.getHistoryAuctionPreviews(userId);
      boolean fullSnapshot = sinceVersion < 0;
      if (!fullSnapshot) {
        auctions = auctions.stream().filter(auction -> auction.version() > sinceVersion).toList();
      }
      AuctionHistoryResponse response = new AuctionHistoryResponse(auctions, fullSnapshot);
      clientHandler.sendPacket(
          PacketRes.of(ResponseType.FETCH_AUCTION_HISTORY_RESULT, "OK", response));
    } catch (Exception e) {
      logger.error("Failed to fetch auction history", e);
      clientHandler.sendPacket(
          PacketRes.error(
              ResponseType.FETCH_AUCTION_HISTORY_RESULT, "Không thể tải lịch sử đấu giá"));
    }
  }
}
