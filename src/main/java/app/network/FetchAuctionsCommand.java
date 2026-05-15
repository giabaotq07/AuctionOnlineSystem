package app.network;

import app.dto.AuctionSummary;
import app.dto.AuctionsResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionsCommand. */
public class FetchAuctionsCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionsCommand.class);
  private final AuctionService auctionService;

  /** FetchAuctionsCommand. */
  public FetchAuctionsCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      List<AuctionSummary> summaries = auctionService.getAuctionSummaries();
      AuctionsResponse response = new AuctionsResponse(true, "OK", summaries);
      clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_AUCTIONS, response));
    } catch (Exception e) {
      logger.error("Failed to fetch auctions", e);
      clientHandler.sendPacket(
          PacketRes.error(PacketType.FETCH_AUCTIONS, "Không thể tải danh sách đấu giá"));
    }
  }
}
