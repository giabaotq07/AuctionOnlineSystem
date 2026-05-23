package app.server.command;

import app.common.dto.AuctionDetailResponse;
import app.common.dto.UpdateAuctionRequest;
import app.common.enums.AuctionStatus;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.models.Auction;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.AuctionScheduler;
import app.server.service.AuctionService;

/** UpdateAuctionCommand. */
public class UpdateAuctionCommand extends SafeCommand {
  private final AuctionService auctionService;
  private final AuctionQueryService auctionQueryService;

  /** UpdateAuctionCommand. */
  public UpdateAuctionCommand(
      AuctionService auctionService, AuctionQueryService auctionQueryService) {
    this.auctionService = auctionService;
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    UpdateAuctionRequest request =
        requirePayload(packet, UpdateAuctionRequest.class, "Dữ liệu phiên đấu giá không hợp lệ.");
    if (request.auctionId() <= 0 || request.expectedVersion() < 0) {
      throw new ValidationException("Dữ liệu phiên đấu giá không hợp lệ.");
    }
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
            requireUser(clientHandler));
    AuctionDetailResponse response = buildResponse(auction);
    sendSuccess(clientHandler, "Cập nhật phiên thành công.", response);
    notifyAfterUpdate(auction, response);
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.UPDATE_AUCTION_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể cập nhật phiên đấu giá.";
  }

  private AuctionDetailResponse buildResponse(Auction auction) {
    try {
      return new AuctionDetailResponse(
          app.common.mapper.ModelMapper.toAuctionDto(
              auctionQueryService.getAuctionDetail(auction.getId())));
    } catch (Exception e) {
      logger.warn("Updated auction {}, but failed to load detail response", auction.getId(), e);
      return new AuctionDetailResponse(app.common.mapper.ModelMapper.toAuctionDto(auction));
    }
  }

  private void notifyAfterUpdate(Auction auction, AuctionDetailResponse response) {
    try {
      scheduleStartIfNeeded(auction);
    } catch (Exception e) {
      logger.warn("Auction {} was updated, but scheduling failed", auction.getId(), e);
    }
    try {
      Server.broadcastToAuctionViewers(
          auction.getId(), PacketRes.of(ResponseType.AUCTION_DETAIL_UPDATED, "OK", response), -1);
      Server.broadcastAuctionList(auctionQueryService);
    } catch (Exception e) {
      logger.warn(
          "Auction {} was updated, but post-update notification failed", auction.getId(), e);
    }
  }

  private void scheduleStartIfNeeded(Auction auction) {
    if (auction.getStatus() == AuctionStatus.OPEN && auction.getStartTime() != null) {
      AuctionScheduler.getInstance().scheduleStart(auction.getId(), auction.getStartTime());
    }
  }
}
