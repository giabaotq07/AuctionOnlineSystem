package app.network;

import app.dto.AuctionResultRequest;
import app.dto.AuctionResultResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionResultCommand. */
public class FetchAuctionResultCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(FetchAuctionResultCommand.class);
  private final AuctionService auctionService;

  /** FetchAuctionResultCommand. */
  public FetchAuctionResultCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      AuctionResultRequest request = packet.getData(AuctionResultRequest.class);
      if (request == null) {
        sendError(clientHandler, "Dữ liệu yêu cầu không hợp lệ.");
        return;
      }
      int auctionId = request.auctionId();
      if (auctionId <= 0) {
        sendError(clientHandler, "auctionId không hợp lệ.");
        return;
      }
      AuctionResultResponse response = auctionService.getAuctionResult(auctionId);
      if (response == null) {
        sendError(clientHandler, "Không tìm thấy kết quả đấu giá.");
        return;
      }
      clientHandler.sendPacket(PacketRes.of(PacketType.FETCH_AUCTION_RESULT, response));
    } catch (ServiceException e) {
      logger.warn("Fetch auction result failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("[SERVER] Fetch auction result error", e);
      sendError(clientHandler, "Không thể lấy kết quả đấu giá.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.FETCH_AUCTION_RESULT, message));
  }
}
