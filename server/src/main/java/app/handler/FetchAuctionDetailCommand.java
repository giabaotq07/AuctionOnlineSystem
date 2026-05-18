package app.handler;

import app.dto.AuctionDetailRequest;
import app.dto.AuctionDetailResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.mapper.DtoMapper;
import app.models.Auction;
import app.models.Item;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import app.service.ItemService;
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
