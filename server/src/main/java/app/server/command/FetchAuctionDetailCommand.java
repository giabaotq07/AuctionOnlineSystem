package app.server.command;

import app.common.dto.AuctionDetailRequest;
import app.common.dto.AuctionDetailResponse;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionDetailCommand. */
public class FetchAuctionDetailCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionDetailCommand.class);
  private final AuctionService auctionService;

  /** FetchAuctionDetailCommand. */
  public FetchAuctionDetailCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      AuctionDetailRequest request = packet.getData(AuctionDetailRequest.class);
      if (request == null) {
        clientHandler.sendPacket(
            PacketRes.error(ResponseType.FETCH_AUCTION_DETAIL_RESULT, "Invalid request"));
        return;
      }
      if (request.auctionId() <= 0) {
        clientHandler.sendPacket(
            PacketRes.error(ResponseType.FETCH_AUCTION_DETAIL_RESULT, "Invalid auction id"));
        return;
      }
      clientHandler.getSession().setViewingAuctionId(request.auctionId());
      if (auctionService.isAuctionVersionCurrent(request.auctionId(), request.knownVersion())) {
        AuctionDetailResponse response =
            AuctionDetailResponse.notModified(request.auctionId(), request.knownVersion());
        clientHandler.sendPacket(
            PacketRes.of(ResponseType.FETCH_AUCTION_DETAIL_RESULT, "OK", response));
        return;
      }
      AuctionDetailResponse response =
          new AuctionDetailResponse(auctionService.getAuctionDetail(request.auctionId()));
      clientHandler.sendPacket(
          PacketRes.of(ResponseType.FETCH_AUCTION_DETAIL_RESULT, "OK", response));
    } catch (ServiceException e) {
      logger.warn("Fetch auction detail failed: {}", e.getMessage());
      clientHandler.sendPacket(
          PacketRes.error(ResponseType.FETCH_AUCTION_DETAIL_RESULT, e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error while fetching auction detail", e);
      clientHandler.sendPacket(
          PacketRes.error(ResponseType.FETCH_AUCTION_DETAIL_RESULT, "Internal server error"));
    }
  }
}
