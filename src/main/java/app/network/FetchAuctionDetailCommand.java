package app.network;

import app.dto.AuctionDetail;
import app.dto.AuctionDetailRequest;
import app.dto.AuctionDetailResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
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
            PacketRes.error(PacketType.FETCH_AUCTION_DETAIL, "Invalid request"));
        return;
      }
      if (request.auctionId() <= 0) {
        clientHandler.sendPacket(
            PacketRes.error(PacketType.FETCH_AUCTION_DETAIL, "Invalid auction id"));
        return;
      }
      AuctionDetail detail = auctionService.getAuctionDetail(request.auctionId());
      AuctionDetailResponse response = new AuctionDetailResponse(detail);
      clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_AUCTION_DETAIL, response));
    } catch (ServiceException e) {
      logger.warn("Fetch auction detail failed: {}", e.getMessage());
      clientHandler.sendPacket(PacketRes.error(PacketType.FETCH_AUCTION_DETAIL, e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error while fetching auction detail", e);
      clientHandler.sendPacket(
          PacketRes.error(PacketType.FETCH_AUCTION_DETAIL, "Internal server error"));
    }
  }
}
