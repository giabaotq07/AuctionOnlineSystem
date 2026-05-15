package app.network;

import app.data.CancelAuctionRequest;
import app.data.CancelAuctionResponse;
import app.enums.PacketType;
import app.exception.ServiceException;
import app.models.PacketReq;
import app.models.PacketRes;
import app.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CancelAuctionCommand. */
public class CancelAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CancelAuctionCommand.class);
  private final AuctionService auctionService;

  /** CancelAuctionCommand. */
  public CancelAuctionCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    int auctionId = 0;
    try {
      CancelAuctionRequest request = packet.getData(CancelAuctionRequest.class);
      if (request == null || request.auctionId() <= 0 || request.expectedVersion() < 0) {
        sendError(clientHandler, auctionId, "Dữ liệu phiên đấu giá không hợp lệ.");
        return;
      }
      auctionId = request.auctionId();
      auctionService.cancelAuctionByAdmin(
          auctionId, clientHandler.getUser().getId(), request.expectedVersion());
      clientHandler.sendPacket(
          PacketRes.of(
              PacketType.CANCEL_AUCTION,
              new CancelAuctionResponse(true, "Hủy phiên thành công.", auctionId)));
      Server.broadcast(
          PacketRes.of(
              PacketType.CANCEL_AUCTION,
              new CancelAuctionResponse(true, "Phiên đã bị hủy.", auctionId)),
          clientHandler.getUser().getId());
    } catch (ServiceException e) {
      logger.warn("Cancel auction failed: {}", e.getMessage());
      sendError(clientHandler, auctionId, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected cancel auction error", e);
      sendError(clientHandler, auctionId, "Không thể hủy phiên đấu giá.");
    }
  }

  private void sendError(ClientHandler clientHandler, int auctionId, String message) {
    clientHandler.sendPacket(
        PacketRes.of(
            false,
            PacketType.CANCEL_AUCTION,
            new CancelAuctionResponse(false, message, auctionId)));
  }
}
