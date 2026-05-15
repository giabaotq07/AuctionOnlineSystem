package app.network;

import app.data.AuctionSummary;
import app.data.HistoryRequest;
import app.data.HistoryResponse;
import app.enums.PacketType;
import app.models.PacketReq;
import app.models.PacketRes;
import app.models.User;
import app.service.AuctionService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchHistoryCommand. */
public class FetchHistoryCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchHistoryCommand.class);
  private final AuctionService auctionService;

  /** FetchHistoryCommand. */
  public FetchHistoryCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        clientHandler.sendPacket(
            PacketRes.error(PacketType.FETCH_HISTORY, "Authentication required"));
        return;
      }
      HistoryRequest request = packet.getData(HistoryRequest.class);
      if (request == null) {
        clientHandler.sendPacket(PacketRes.error(PacketType.FETCH_HISTORY, "Invalid request"));
        return;
      }
      User user = clientHandler.getUser();
      // KHÔNG trust userId từ client
      int userId = user.getId();
      List<AuctionSummary> summaries = auctionService.getHistorySummaries(userId);
      HistoryResponse response = new HistoryResponse(true, "OK", summaries);
      clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_HISTORY, response));
    } catch (Exception e) {
      logger.error("Failed to fetch history", e);
      clientHandler.sendPacket(
          PacketRes.error(PacketType.FETCH_HISTORY, "Không thể tải lịch sử đấu giá"));
    }
  }
}
