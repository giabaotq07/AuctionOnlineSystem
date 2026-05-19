package app.server.handler;

import app.common.dto.AuctionDetailRequest;
import app.common.dto.AuctionDetailResponse;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.Auction;
import app.common.models.Item;
import app.common.models.PacketReq;
import app.common.models.PacketRes;
import app.server.service.AuctionService;
import app.server.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionDetailCommand. */
public class FetchAuctionDetailCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionDetailCommand.class);
  private final AuctionService auctionService;
  private final ItemService itemService;

  /** FetchAuctionDetailCommand. */
  public FetchAuctionDetailCommand(AuctionService auctionService, ItemService itemService) {
    this.auctionService = auctionService;
    this.itemService = itemService;
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
      Auction auction = auctionService.getAuctionById(request.auctionId());
      Item item =
          itemService
              .getById(auction.getItemId())
              .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
      AuctionDetailResponse response =
          new AuctionDetailResponse(DtoMapper.toAuctionDetail(auction, item));
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
