package app.server.command;

import app.common.dto.AuctionDetailResponse;
import app.common.dto.UpdateAuctionRequest;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UpdateAuctionCommand. */
public class UpdateAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(UpdateAuctionCommand.class);
  private final AuctionService auctionService;

  /** UpdateAuctionCommand. */
  public UpdateAuctionCommand(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    int auctionId = 0;
    try {
      UpdateAuctionRequest request = packet.getData(UpdateAuctionRequest.class);
      if (request == null || request.auctionId() <= 0 || request.expectedVersion() < 0) {
        sendError(clientHandler, "Dữ liệu phiên đấu giá không hợp lệ.");
        return;
      }
      auctionId = request.auctionId();
      var user = clientHandler.getUser();
      var detail =
          auctionService.updateAuctionWithItem(
              request.auctionId(),
              request.name(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type(),
              request.durationMinutes(),
              request.startTime(),
              user.getId(),
              user.getRole(),
              request.expectedVersion());
      AuctionDetailResponse response = new AuctionDetailResponse(detail);
      clientHandler.sendPacket(
          PacketRes.of(ResponseType.UPDATE_AUCTION_RESULT, "Cập nhật phiên thành công.", response));
      Server.broadcastToAuctionViewers(
          auctionId, PacketRes.of(ResponseType.AUCTION_DETAIL_UPDATED, "OK", response), -1);
      Server.broadcastAuctionList(auctionService);
    } catch (ServiceException e) {
      logger.warn("Update auction failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected update auction error", e);
      sendError(clientHandler, "Không thể cập nhật phiên đấu giá.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(ResponseType.UPDATE_AUCTION_RESULT, message));
  }
}
