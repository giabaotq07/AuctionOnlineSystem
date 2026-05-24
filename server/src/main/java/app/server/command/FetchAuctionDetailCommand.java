package app.server.command;

import app.common.dto.AuctionDetailRequest;
import app.common.dto.AuctionDetailResponse;
import app.common.dto.SetAutoBidResponse;
import app.common.enums.ResponseType;
import app.common.exception.ValidationException;
import app.common.models.Auction;
import app.common.models.AutoBid;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.AuctionQueryService;
import app.server.service.AutoBidService;

/** FetchAuctionDetailCommand. */
public class FetchAuctionDetailCommand extends SafeCommand {
  private final AuctionQueryService auctionQueryService;
  private final AutoBidService autoBidService;

  /** FetchAuctionDetailCommand. */
  public FetchAuctionDetailCommand(
      AuctionQueryService auctionQueryService, AutoBidService autoBidService) {
    this.auctionQueryService = auctionQueryService;
    this.autoBidService = autoBidService;
  }

  @Override
  protected void doExecute(ClientHandler clientHandler, PacketReq packet) {
    AuctionDetailRequest request =
        requirePayload(packet, AuctionDetailRequest.class, "Dữ liệu phiên đấu giá không hợp lệ.");
    if (request.auctionId() <= 0) {
      throw new ValidationException("Phiên đấu giá không hợp lệ.");
    }
    clientHandler.getSession().setViewingAuctionId(request.auctionId());
    Auction currentAuction = auctionQueryService.getAuction(request.auctionId());
    if (request.knownVersion() >= 0 && currentAuction.getVersion() == request.knownVersion()) {
      AuctionDetailResponse response =
          AuctionDetailResponse.notModified(request.auctionId(), request.knownVersion());
      sendSuccess(clientHandler, "OK", response);
    } else {
      currentAuction = auctionQueryService.getAuctionDetail(request.auctionId());
      AuctionDetailResponse response =
          new AuctionDetailResponse(app.common.mapper.ModelMapper.toAuctionDto(currentAuction));
      sendSuccess(clientHandler, "OK", response);
    }

    // Send the user's active auto-bid for this auction if logged in
    try {
      if (clientHandler.getSession().getUser() != null) {
        int userId = clientHandler.getSession().getUser().getId();
        AutoBid autoBid = autoBidService.getAutoBid(request.auctionId(), userId).orElse(null);
        SetAutoBidResponse autoBidResponse =
            new SetAutoBidResponse(
                request.auctionId(),
                autoBid != null ? autoBid.getMaxAmount() : 0L,
                autoBid != null ? autoBid.getIncrementAmount() : 0L,
                autoBid != null && autoBid.isEnabled(),
                currentAuction.getHighestBid(),
                currentAuction.getWinnerId() == null ? 0 : currentAuction.getWinnerId());
        clientHandler.sendPacket(
            PacketRes.of(ResponseType.SET_AUTO_BID_RESULT, "OK", autoBidResponse));
      }
    } catch (Exception e) {
      logger.warn("Failed to send active auto-bid to client on room entry", e);
    }
  }

  @Override
  protected ResponseType responseType() {
    return ResponseType.AUCTION_RESULT;
  }

  @Override
  protected String unexpectedErrorMessage() {
    return "Không thể tải chi tiết đấu giá.";
  }
}
