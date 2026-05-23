package app.server.command;

import app.common.dto.*;
import app.common.enums.AuctionStatus;
import app.common.enums.ResponseType;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.AuctionQueryService;
import app.server.service.AuctionScheduler;
import app.server.service.AuctionService;

/** CreateAuctionCommand. */
public class CreateAuctionCommand extends SafeCommand {
  private final AuctionService auctionService;
  private final AuctionQueryService auctionQueryService;

  /** CreateAuctionCommand. */
  public CreateAuctionCommand(
      AuctionService auctionService, AuctionQueryService auctionQueryService) {
    this.auctionService = auctionService;
    this.auctionQueryService = auctionQueryService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    CreateAuctionRequest request =
        requirePayload(packet, CreateAuctionRequest.class, "Invalid request");
    User user = requireUser(clientHandler);
    Auction auction =
        auctionService.createAuction(
            request.name(),
            request.description(),
            request.startingPrice(),
            request.stepPrice(),
            request.type(),
            request.durationMinutes(),
            request.startTime(),
            user);
    CreateAuctionResponse response = buildResponse(auction);
    sendSuccess(clientHandler, "Tạo phiên thành công", response);
    notifyAfterCreate(auction, response, user.getId());
    logger.info("Auction created successfully by user {}", user.getId());
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.CREATE_AUCTION_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Tạo phiên thất bại";
  }

  private CreateAuctionResponse buildResponse(Auction auction) {
    try {
      return new CreateAuctionResponse(
          app.common.mapper.ModelMapper.toAuctionDto(
              auctionQueryService.getAuctionDetail(auction.getId())));
    } catch (Exception e) {
      logger.warn("Created auction {}, but failed to load detail response", auction.getId(), e);
      return new CreateAuctionResponse(app.common.mapper.ModelMapper.toAuctionDto(auction));
    }
  }

  private void notifyAfterCreate(Auction auction, CreateAuctionResponse response, int actorId) {
    try {
      scheduleStartIfNeeded(auction);
    } catch (Exception e) {
      logger.warn("Auction {} was created, but scheduling failed", auction.getId(), e);
    }
    try {
      Server.broadcast(
          PacketRes.of(ResponseType.AUCTION_CREATED, "Có phiên đấu giá mới.", response), actorId);
      Server.broadcastAuctionList(auctionQueryService);
    } catch (Exception e) {
      logger.warn(
          "Auction {} was created, but post-create notification failed", auction.getId(), e);
    }
  }

  private void scheduleStartIfNeeded(Auction auction) {
    if (auction.getStatus() == AuctionStatus.OPEN && auction.getStartTime() != null) {
      AuctionScheduler.getInstance().scheduleStart(auction.getId(), auction.getStartTime());
    }
  }
}
