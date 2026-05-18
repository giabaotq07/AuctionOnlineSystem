package app.handler;

import app.dto.AuctionSummariesResponse;
import app.dto.AuctionSummary;
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

import java.util.List;

/** FetchAuctionSummariesCommand. */
public class FetchAuctionSummariesCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionSummariesCommand.class);
  private final AuctionService auctionService;
  private final ItemService itemService;

  /** FetchAuctionSummariesCommand. */
  public FetchAuctionSummariesCommand(AuctionService auctionService, ItemService itemService) {
    this.auctionService = auctionService;
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      List<AuctionSummary> summaries =
          auctionService.getAllAuctions().stream().map(this::toSummary).toList();
      AuctionSummariesResponse response = new AuctionSummariesResponse(summaries);
      clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_AUCTION_SUMMARIES, response));
    } catch (Exception e) {
      logger.error("Failed to fetch auctions", e);
      clientHandler.sendPacket(
          PacketRes.error(PacketType.FETCH_AUCTION_SUMMARIES, "Không thể tải danh sách đấu giá"));
    }
  }

  private AuctionSummary toSummary(Auction auction) {
    Item item =
        itemService
            .getById(auction.getItemId())
            .orElseThrow(() -> new ServiceException("Không tìm thấy vật phẩm."));
    return DtoMapper.toAuctionSummary(auction, item);
  }
}
