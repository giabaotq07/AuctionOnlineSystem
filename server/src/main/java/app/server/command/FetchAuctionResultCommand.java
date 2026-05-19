package app.server.command;

import app.common.dto.AuctionResultRequest;
import app.common.dto.AuctionResultResponse;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.BidTransaction;
import app.common.models.PacketReq;
import app.common.models.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AuctionService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FetchAuctionResultCommand. */
public class FetchAuctionResultCommand extends Command {
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
      Optional<BidTransaction> highestBid = auctionService.completeAndGetHighestBid(auctionId);
      AuctionResultResponse response = DtoMapper.toAuctionResultResponse(auctionId, highestBid);
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
