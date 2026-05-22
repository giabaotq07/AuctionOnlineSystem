package app.server.command;

import app.common.dto.AuctionDetailResponse;
import app.common.dto.UpdateAuctionRequest;
import app.common.enums.AuctionStatus;
import app.common.enums.ResponseType;
import app.common.exception.ServiceException;
import app.common.models.Auction;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.AuctionScheduler;
import app.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** UpdateAuctionCommand. */
public class UpdateAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(UpdateAuctionCommand.class);
  private final AuctionService auctionService;
  private final AuctionQueryService auctionQueryService;

  /** UpdateAuctionCommand. */
  public UpdateAuctionCommand(
      AuctionService auctionService, AuctionQueryService auctionQueryService) {
    this.auctionService = auctionService;
    this.auctionQueryService = auctionQueryService;
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
      Auction auction =
          auctionService.updateAuction(
              request.auctionId(),
              request.name(),
              request.description(),
              request.startingPrice(),
              request.stepPrice(),
              request.type(),
              request.durationMinutes(),
              request.startTime(),
              request.expectedVersion(),
              user);
      scheduleStartIfNeeded(auction);
      var detail = auctionQueryService.getAuctionDetail(auction.getId());
      AuctionDetailResponse response =
          new AuctionDetailResponse(app.common.mapper.ModelMapper.toAuctionDto(detail));
      clientHandler.sendPacket(
          PacketRes.of(ResponseType.UPDATE_AUCTION_RESULT, "Cập nhật phiên thành công.", response));
      Server.broadcastToAuctionViewers(
          auctionId, PacketRes.of(ResponseType.AUCTION_DETAIL_UPDATED, "OK", response), -1);
      Server.broadcastAuctionList(auctionQueryService);
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

  private void scheduleStartIfNeeded(Auction auction) {
    if (auction.getStatus() == AuctionStatus.OPEN && auction.getStartTime() != null) {
      AuctionScheduler.getInstance().scheduleStart(auction.getId(), auction.getStartTime());
    }
  }
}
