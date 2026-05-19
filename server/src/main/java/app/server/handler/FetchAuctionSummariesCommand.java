package app.server.handler;

import app.common.dto.AuctionSummariesResponse;
import app.common.dto.AuctionSummary;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.Auction;
import app.common.models.Item;
import app.common.models.PacketReq;
import app.common.models.PacketRes;
import app.server.service.AuctionService;
import app.server.service.ItemService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
