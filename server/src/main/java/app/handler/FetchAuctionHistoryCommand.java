package app.handler;

import app.dto.AuctionHistoryResponse;
import app.dto.AuctionSummary;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.mapper.DtoMapper;
import app.models.*;
import app.service.AuctionService;
import app.service.ItemService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionHistoryCommand. */
public class FetchAuctionHistoryCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionHistoryCommand.class);
  private final AuctionService auctionService;
  private final ItemService itemService;

  /** FetchAuctionHistoryCommand. */
  public FetchAuctionHistoryCommand(AuctionService auctionService, ItemService itemService) {
    this.auctionService = auctionService;
    this.itemService = itemService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        clientHandler.sendPacket(
            PacketRes.error(PacketType.FETCH_AUCTION_HISTORY, "Authentication required"));
        return;
      }
      User user = clientHandler.getUser();
      // KHÔNG trust userId từ client
      int userId = user.getId();
      List<AuctionSummary> summaries =
          auctionService.getHistoryAuctions(userId).stream().map(this::toSummary).toList();
      AuctionHistoryResponse response = new AuctionHistoryResponse(summaries);
      clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_AUCTION_HISTORY, response));
    } catch (Exception e) {
      logger.error("Failed to fetch history", e);
      clientHandler.sendPacket(
          PacketRes.error(PacketType.FETCH_AUCTION_HISTORY, "Không thể tải lịch sử đấu giá"));
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
